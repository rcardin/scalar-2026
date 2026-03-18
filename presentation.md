# The Concurrency Triangle: Scala Fibers, Java Virtual Threads, and Kotlin Coroutines

## Slide Deck — Scalar 2026

---

## Slide 1: Title

**The Concurrency Triangle**
Scala Fibers, Java Virtual Threads, and Kotlin Coroutines

Riccardo Cardin
Scalar 2026

> **Speaker notes:** Welcome everyone. Today we're going to look at how three JVM languages solve the same fundamental problem — and spoiler: they all end up at the same place, just at different abstraction levels.

---

## Slide 2: Agenda

1. The Problem
2. Scala Fibers — Continuations at the user level
3. Kotlin Coroutines — Continuations at compile time
4. Java Virtual Threads — Continuations at runtime
5. References

> **Speaker notes:** We'll start by understanding why OS threads are a bottleneck, then see how Scala, Kotlin, and Java each solve it. The key insight: all three implement the same pattern — continuations on thread pools. They just do it at different abstraction levels.

---

## Slide 3: Concurrency vs. Parallelism

- **Concurrency** is a semantic feature **of a problem**
  - We have many **tasks** that can be **interleaved**
  - The order of execution is not important
  - Tasks are still **sequential** — the runtime interleaves them
  - We can have concurrency with a **single executor** (single core)

```
Task 1:     [1] [2] [3]
Task 2:     [a] [b] [c]

Execution:  [1] [2] [a] [3] [b] [c]   ← one executor, interleaved
```

- **Parallelism** is a property **of the runtime**
  - The execution environment has enough **resources** to run tasks **at the same time**

```
Executor 1: [1] [2] [3]
Executor 2: [a] [b] [c]   ← two executors, truly simultaneous
```

- Our problem today is about **concurrency**: how to efficiently interleave many tasks on a limited number of threads

> **Speaker notes:** Before we dive in, let's be precise about terminology. Concurrency is about the PROBLEM — we have tasks that can run in any order. Parallelism is about the RUNTIME — we have enough CPUs to run them simultaneously. You can have concurrency without parallelism: one core juggling many tasks. What we're solving today is a concurrency problem. We have thousands of tasks — HTTP requests, database queries — and we need to interleave them efficiently on a limited number of threads. The question is: who manages that interleaving, and at what cost?

---

## Slide 4: Threads Block — And That's the Problem

- When a thread calls I/O (DB query, HTTP request), it **blocks**
  - The **entire thread stack** is frozen — the OS cannot reuse it
  - The OS **unmounts** the thread from the CPU core
  - Another thread is **mounted**: this is a **context switch**
- Context switching is **expensive**
  - Kernel transition, CPU cache invalidation, register save/restore
- The OS is doing its best — but it has **no idea** what our program wants to do next
  - It treats threads as opaque: save ALL the state, restore ALL the state
  - It's a **heavyweight** operation for what's often just "call me back when the I/O is done"

```
Task A: [ work ][ wait for I/O... ][ work ]
Thread:  ───────┤                  ├───────
                ↓ OS context switch ↓
                 (save entire stack, (restore entire stack,
                  load another one)   resume from where
                                      we left off)
```

> **Speaker notes:** Let's look at what actually happens when a thread blocks. Say it's calling a database. The thread just sits there. The OS has no choice — it saves all the registers, the entire stack, and loads another thread. That's a context switch. It involves a kernel transition, invalidating CPU caches, and moving kilobytes of data around. And the OS has to do this because it doesn't know anything about your program. It treats the thread as a black box — save everything, restore everything. That's the fundamental problem.

---

## Slide 5: What If WE Manage the Scheduling?

- The OS doesn't know what our tasks need — but **we do**
- Key idea: instead of freezing the whole thread, just **save what to do next**
  - This is called a **continuation** — "the rest of the computation"
  - It's **lightweight**: just a function (or a small data structure), not an entire thread stack

```scala
// The continuation is implicit (it's the next line)
val result = add(1, multiply(2, 3))
println(s"The result is: $result")

// Continuation-Passing Style — the continuation is explicit (it's the callback)
multiplyCPS(2, 3) { multiplyResult =>
  addCPS(1, multiplyResult) { addResult =>
    println(s"The result is: $addResult")
  }
}
```

- If we reify "what to do next" into a value, we can:
  - **Suspend** a task: save its continuation, **free the thread**
  - **Resume** it later: pick up the continuation and run it — on **any** thread
- The thread is never blocked, never wasted — **no OS context switch**

> **Speaker notes:** Here's the key insight. The OS does a heavyweight operation because it doesn't know what our program needs. But WE know. When our task is waiting for I/O, we know exactly what should happen next — it's the next line of code, the callback, the "continuation". What if instead of letting the OS freeze the whole thread, we just save that tiny piece of information — "what to do next" — and free the thread to run other tasks? That's a continuation. It's just a function. And this is the fundamental idea behind fibers, coroutines, and virtual threads. Instead of OS context switching, we do USER-SPACE task switching — save a small continuation on the heap, not an entire thread stack in the kernel.

---

## Slide 6: What We Need

A continuation-based runtime needs four things:

1. A definition of what a **continuation** is
2. A **thread pool** to run continuations
3. A **scheduler** to pick the next continuation to run
4. A way to **yield** or **suspend** a continuation

Let's see how **Scala**, **Kotlin**, and **Java** implement each of these.

> **Speaker notes:** So here's our checklist. Every solution we'll see today needs these four ingredients. The difference is WHERE each language implements them. Scala does it in user-space libraries. Kotlin does it at compile time. Java does it inside the JVM runtime. Same recipe, different kitchens.

---

## Slide 7: Section Divider — Scala Fibers

### 01

**Scala Fibers**
Continuations at the User Level

---

## Slide 8: IO — Describing a Program as Data

- In Scala, we can **describe** a computation without executing it
- The `IO` monad is an **ADT** — a tree of instructions

```scala
enum IO[+A]:
  case Pure(value: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](io: IO[A], cont: A => IO[B]) extends IO[B]
```

- `Pure` — a completed computation (a value)
- `Delay` — a lazy computation (a thunk)
- `FlatMap` — a computation followed by **what to do next**: a function `A => IO[B]`

```scala
extension [A](io: IO[A])
  def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(io, f)
  def map[B](f: A => B): IO[B] = FlatMap(io, a => Pure(f(a)))

object IO:
  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)
```

> **Speaker notes:** Here's where Scala shines. In the effect system approach, we don't run code immediately. We build a data structure that DESCRIBES the computation. Look at FlatMap: it stores an IO and a function `A => IO[B]`. That function is "what to do next with the result." Each flatMap call adds a link to the chain. But — and this is important — this is just a description. A sequential program as data. It doesn't solve our concurrency problem yet. Let's see why.

---

## Slide 9: FlatMap Chains — Sequential, Not Yet Concurrent

- A program built with `flatMap` is a **chain of steps**

```scala
val bathTime: IO[Unit] =
  IO.delay(println("Going to the bathroom"))
    .flatMap(_ => IO.delay(Thread.sleep(500)))  // step 2 — still blocks!
    .flatMap(_ => IO.delay(println("Done!")))   // step 3
```

```
FlatMap
├── FlatMap
│   ├── Delay(() => println("Going to the bathroom"))
│   └── step₂: _ => Delay(() => Thread.sleep(500))
└── step₃: _ => Delay(() => println("Done!"))
```

- Each `flatMap` stores "what to do next" — that's a **continuation** in the CS sense
- But this chain is **purely sequential**: run step 1, then step 2, then step 3
- If step 2 blocks (e.g., `Thread.sleep`), the thread is **still blocked**
- We have continuations, but we're **not using them for concurrency yet**

> **Speaker notes:** Look at this tree. Each flatMap stores a function — what to do next. That IS a continuation in the computer science sense. But here's the thing: if we just interpret this chain top to bottom, it's no different from regular sequential code. When we hit Thread.sleep, the thread blocks. We haven't solved anything yet. We have the building blocks — a program described as data with explicit "next steps" — but we need one more ingredient to actually suspend and resume.

---

## Slide 10: The Run Loop — A Trampoline on the Heap

- The runtime **interprets** the IO tree using a **trampoline**

```scala
def unsafeRun[A](io: IO[A]): A =
  val stack = scala.collection.mutable.Stack[Any => IO[Any]]()
  var current: IO[Any] = io

  while true do
    current match
      case Pure(value) =>
        if stack.isEmpty then return value.asInstanceOf[A]
        else current = stack.pop()(value)

      case Delay(thunk) =>
        current = Pure(thunk())

      case FlatMap(inner, cont) =>
        stack.push(cont.asInstanceOf[Any => IO[Any]])
        current = inner

  throw new RuntimeException("Unreachable")
```

- The **stack of continuations** lives on the **heap**, not on the thread stack
- This is important: the execution state is now a data structure **we control**
- We can **pause** it, **save** it, and **resume** it later — if we add a way to suspend

> **Speaker notes:** Here's the interpreter. Instead of recursive calls that eat thread stack, we use a while loop and an explicit stack of continuations on the heap. FlatMap? Push the continuation, process the inner IO. Pure value? Pop the next continuation. This is trampolining. And here's the key insight: our execution state — the stack of "what to do next" — is now a regular object on the heap. We OWN it. We can pause this loop, save the stack, free the thread, and resume later. We have the infrastructure for suspension. We just need a way to trigger it.

---

## Slide 11: Async — The Real Continuation

- The outside world is full of **callback-based APIs**: timers, sockets, HTTP clients, ...
- `Async` is a **Foreign Function Interface** (FFI): it bridges callbacks into the IO world

```scala
enum IO[+A]:
  case Pure(value: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](io: IO[A], cont: A => IO[B]) extends IO[B]
  case Async(register: (Either[Throwable, A] => Unit) => Unit)
```

- The `register` function receives a **callback** `cb: Either[Throwable, A] => Unit`
- The callback **IS the continuation** — it's how the async world tells our fiber: "I'm done, here's the result, **resume your computation**"

```scala
def sleep(millis: Long): IO[Unit] =
  IO.Async { cb =>                          // cb is the continuation!
    scheduler.schedule(
      () => cb(Right(())),                  // "I'm done, resume the fiber"
      millis,
      TimeUnit.MILLISECONDS
    )
  }

// Converting a Future — same pattern
def fromFuture[A](future: => Future[A]): IO[A] =
  IO.Async { cb =>
    future.onComplete {
      case Success(a) => cb(Right(a))       // Resume with value
      case Failure(e) => cb(Left(e))        // Resume with error
    }
  }
```

- When the run loop hits `Async`:
  1. It **stops** the trampoline — the continuation stack stays on the heap
  2. It **frees the thread** — the thread can run other fibers
  3. When `cb` is called, the fiber is **rescheduled** with its saved continuations
- `FlatMap` builds the chain; `Async` is where we **cut it**, free the thread, and **resume later**

> **Speaker notes:** HERE is where the continuation pattern actually solves our concurrency problem. Async is an FFI — a Foreign Function Interface. The outside world speaks in callbacks: Future.onComplete, timer.schedule, socket.read. Async bridges that world into IO. And look at the callback `cb` — it's the continuation! It's "what to do when the async operation completes." When the run loop hits Async, it hands `cb` to the external API and STOPS. The thread walks away. The continuation stack sits on the heap, waiting. When the external operation completes, it calls `cb`, which re-submits the fiber to the scheduler with its full continuation stack. The fiber resumes exactly where it left off. FlatMap gave us the structure — a program described as data. Async gives us the actual continuation — the bridge between the callback world and our fiber world.

---

## Slide 12: Fibers — Putting It All Together

- A **Fiber** is the sum of everything we've built:
  - An **IO program** described as data (slides 8-9)
  - A **continuation stack** on the heap — the trampoline (slide 10)
  - The ability to **suspend via Async** and resume via callback (slide 11)
- It's just an object on the heap — **lightweight**, not an OS thread

```scala
class IOFiber[A](io: IO[A], scheduler: Scheduler):
  private var currentIO: IO[Any] = io                   // The current step
  private val continuations = Stack[Any => IO[Any]]()   // What to do next

  def run(): Unit =
    currentIO match
      case Pure(value) =>
        if continuations.nonEmpty then
          currentIO = continuations.pop()(value)
          scheduler.submit(this)              // Continue on the thread pool
      case Delay(thunk) =>
        currentIO = Pure(thunk())
        scheduler.submit(this)
      case FlatMap(inner, cont) =>
        continuations.push(cont)              // Save the continuation
        currentIO = inner
        scheduler.submit(this)
      case Async(register) =>
        register {                            // Suspend! Free the thread
          case Right(value) =>
            currentIO = Pure(value)
            scheduler.submit(this)            // Resume when callback fires
          case Left(error) => /* handle error */
        }
```

- After each step, the fiber **re-submits itself** to the scheduler — **cooperative scheduling**
- At `Async`, the fiber truly **suspends**: no thread is held, the callback will resume it

> **Speaker notes:** And now everything comes together. A Fiber is the combination of all the pieces we've built. It holds the IO program — our data structure. It has a continuation stack — the "what to do next" chain, living on the heap. And it can suspend via Async — freeing the thread when it hits an I/O boundary. Look at the run method: it's our trampoline from slide 10, but now it re-submits itself to the scheduler after each step instead of looping. That's cooperative scheduling — the fiber voluntarily yields, giving other fibers a chance. And when it hits Async, it truly parks. The thread walks away. The callback will wake it up later. A fiber is cheap — it's just an object. You can create millions of them. Each one carries its own continuation stack. That's the Scala approach: we built the entire continuation machinery ourselves, in user space.

---

## Slide 13: The Scheduler — Cooperative Scheduling

```scala
class Scheduler(threadPool: ExecutorService):
  private val queue = ConcurrentLinkedQueue[IOFiber[?]]()

  def submit(fiber: IOFiber[?]): Unit =
    queue.add(fiber)
    threadPool.submit(() => {
      val next = queue.poll()
      if next != null then next.run()
    })
```

- Fibers are enqueued and executed on a **fixed thread pool**
- Threads are **always busy** — no time wasted on blocked fibers
- This is **cooperative scheduling**: fibers yield voluntarily at `Async` boundaries
- No **context switch** at the OS level

> **Speaker notes:** The scheduler is surprisingly simple. It's a queue of fibers and a thread pool. When a fiber yields or completes a step, it goes back in the queue. A thread picks up the next fiber. The threads are never idle — there's always a fiber to run. And because we manage scheduling ourselves in user space, we never pay OS context switch costs.

---

## Slide 14: Putting It All Together — Morning Routine

```scala
val bathTime: IO[Unit] =
  IO.delay(println("Going to the bathroom"))
    .flatMap(_ => sleep(500))
    .flatMap(_ => IO.delay(println("Done with the bath")))

val boilingWater: IO[Unit] =
  IO.delay(println("Boiling some water"))
    .flatMap(_ => sleep(1000))
    .flatMap(_ => IO.delay(println("Water is ready")))

val morningRoutine: IO[Unit] =
  for
    fiberA <- bathTime.start   // Returns IO[Fiber[Unit]]
    fiberB <- boilingWater.start
    _      <- fiberA.join
    _      <- fiberB.join
  yield ()
```

- `start` creates a new `IOFiber` and submits it to the scheduler
- `join` suspends the current fiber until the target completes
- Both tasks run **concurrently** on the same thread pool
- **No OS threads wasted** while `sleep` is waiting

> **Speaker notes:** Here's our morning routine in Scala. We start two fibers — bathTime and boilingWater. Each builds a chain of continuations via flatMap. When they hit `sleep`, they suspend — the thread is freed. The scheduler runs other fibers. When the timer fires, the fiber resumes from its saved continuation. Two concurrent tasks, potentially on a single thread, zero context switches.

---

## Slide 15: The Full Picture — Scala's Continuation Machinery

| Ingredient     | Scala (User Level)                          |
|----------------|---------------------------------------------|
| Continuation   | `Async` callback + `FlatMap` chain          |
| Thread Pool    | `ExecutorService` (e.g., work-stealing pool)|
| Scheduler      | Custom run loop + fiber queue               |
| Yield/Suspend  | `Async` case — callback-based suspension    |

- **Suspension points:** every `Async` boundary — the programmer **explicitly** chooses where to suspend
- **Cats Effect**, **ZIO** implement this with battle-tested optimizations
- **Ox**, **YAES** explore alternative approaches (virtual threads, algebraic effects)

> **Speaker notes:** So here's the recap for Scala. The continuation is the Async callback that resumes a fiber's FlatMap chain. And crucially: the programmer decides where suspension happens — it's every Async boundary. You see it, you control it. Cats Effect and ZIO do this under the hood with years of optimizations. Ox and YAES take different paths. The Scala ecosystem gives you choices.

---

## Slide 16: Section Divider — Kotlin Coroutines

### 02

**Kotlin Coroutines**
Continuations at Compile Time

---

## Slide 17: The Compiler Rewrites Your Code

- You write a **`suspend` function** — the compiler transforms it

```kotlin
// What you write
suspend fun bathTime() {
  logger.info("Going to the bathroom")
  delay(500L)
  logger.info("Done with the bath")
}
```

- The compiler does **two things**:
  1. Adds a `Continuation` parameter — the continuation of **the caller**
  2. Splits the body at every **suspension point** (call to another `suspend fun`)

```kotlin
// What the compiler generates (simplified)
fun bathTime(callerContinuation: Continuation<*>): Any {
  val sm = callerContinuation as? BathTimeSM
    ?: BathTimeSM(callerContinuation)
  if (sm.label == 0) { /* ... */ }
  if (sm.label == 1) { /* ... */ }
}
```

> **Speaker notes:** Kotlin takes a completely different approach. You just mark a function as suspend, and the compiler rewrites it. Two key transformations: it adds a Continuation parameter — the caller's continuation — and it splits the function body at every suspension point. Each segment becomes a branch in a state machine. Let's see the full picture.

---

## Slide 18: The State Machine — Step by Step

```kotlin
fun bathTime(callerContinuation: Continuation<*>): Any {
  val sm = callerContinuation as? BathTimeSM
    ?: BathTimeSM(callerContinuation)     // ① Create state machine
  if (sm.label == 0) {                     // ② First segment
    logger.info("Going to the bathroom")
    sm.label = 1                           // ③ Set resume point
    if (delay(500L, sm) == COROUTINE_SUSPENDED)
      return COROUTINE_SUSPENDED           // ④ Yield the thread!
  }
  if (sm.label == 1) {                     // ⑤ Resumed here
    logger.info("Done with the bath")
  }
}
```

- **First call** (label = 0): runs segment 1, hits `delay`, returns `COROUTINE_SUSPENDED`
- The thread is **free** — no blocking
- **Second call** (label = 1): `delay` calls `sm.resumeWith()`, function **re-enters** at label 1
- The `BathTimeSM` object holds the **state** between calls (label + local variables)

> **Speaker notes:** Let's walk through it. First call: label is 0, we log "going to the bathroom", set label to 1, and call delay. delay is itself a suspend function — it returns COROUTINE_SUSPENDED. So bathTime also returns COROUTINE_SUSPENDED. The thread is free. Later, when the 500ms timer fires, delay calls sm.resumeWith(). This calls bathTime AGAIN with the same state machine object. But now label is 1, so we skip the first block and jump straight to "Done with the bath." The BathTimeSM object is the continuation — it carries the state between calls. Every local variable that needs to survive suspension is stored in it. The function is called multiple times, but the state machine makes it look like a single sequential execution.

---

## Slide 19: resumeWith — The Continuation in Action

- `suspendCoroutine` is a **builder function**: it suspends the coroutine and provides the current `Continuation` to its block
  - You decide **when** to resume by calling `resumeWith`
  - It's Kotlin's equivalent of Scala's `Async` — an FFI to the callback world

```kotlin
suspend fun delay(ms: Long) = suspendCoroutine { continuation ->
    Timer().schedule(object : TimerTask() {
        override fun run() {
            continuation.resumeWith(Result.success(Unit))
        }
    }, ms)
}
```

- Same pattern as Scala's `Async`:

```scala
def sleep(millis: Long): IO[Unit] =
  IO.Async { cb =>
    scheduler.schedule(() => cb(Right(())), millis, MILLISECONDS)
  }
```

- Both register a callback with an external API
- When it fires, **resume the computation** — `resumeWith` / `cb`

> **Speaker notes:** Here's where the parallel with Scala becomes crystal clear. Look at delay: it calls suspendCoroutine, which gives you the continuation object. You hand it to a Timer. When the timer fires, it calls continuation.resumeWith — that's the resume. Now look at Scala's sleep: Async gives you cb, you hand it to a scheduler, and when it fires, cb resumes the fiber. Same exact pattern. Register a callback, resume when done. The continuation is the callback in Scala, and the Continuation object in Kotlin. Different shapes, same role.

---

## Slide 20: Kotlin — Same Ingredients, Compiler-Generated

| Ingredient     | Kotlin (Compile Time)                       |
|----------------|---------------------------------------------|
| Continuation   | `Continuation<T>` interface + `resumeWith`  |
| Thread Pool    | Dispatcher (Default, IO, ...)               |
| Scheduler      | Dispatcher picks where to run               |
| Yield/Suspend  | `COROUTINE_SUSPENDED` return value          |

```kotlin
public interface Continuation<in T> {
  public val context: CoroutineContext
  public fun resumeWith(result: Result<T>)
}
```

- `resumeWith` resumes the caller — **this is CPS**
- **Suspension points:** every call to a `suspend` function — the **compiler** enforces them
- **Coloring problem:** `suspend` is contagious — every caller must also be `suspend`

> **Speaker notes:** Same four ingredients. The Continuation is compiler-generated with resumeWith — that's CPS. The Dispatcher is the scheduler. And here's the key difference from Scala: the suspension points are every call to a suspend function. The compiler enforces this through function coloring — you MUST mark suspendable functions with suspend, and it propagates up the call chain. In Scala, IO has a similar coloring — everything returns IO. But in Kotlin it's enforced by the type system at compile time.

---

## Slide 21: Section Divider — Java Virtual Threads

### 03

**Java Virtual Threads**
Continuations at Runtime

---

## Slide 22: The JVM Does It For You

- Java's approach: reuse the `Thread` API with a different implementation
- `VirtualThread` wraps a `Continuation` object — a **private** JDK class

```java
// What you write — same old Java!
Thread vt = Thread.ofVirtual().start(() -> {
    System.out.println("Going to the bathroom");
    Thread.sleep(Duration.ofMillis(500));
    System.out.println("Done with the bath");
});
```

```java
// Inside VirtualThread (JDK source code)
VirtualThread(Executor scheduler, String name, int characteristics, Runnable task) {
    // ...
    this.scheduler = scheduler;
    this.cont = new VThreadContinuation(this, task);
    this.runContinuation = this::runContinuation;
}
```

- **No coloring**, no special syntax, no monadic wrapping
- `Thread.sleep`, `Socket.read`, `synchronized` — all **yield automatically**

> **Speaker notes:** Java takes the most radical approach. No new syntax. No new types. Just use Thread as you always have, but create it with ofVirtual(). Under the hood, VirtualThread wraps a Continuation object — a JDK-internal class. When the virtual thread calls sleep or does I/O, the JVM's own code calls Continuation.yield(), the stack is copied to the heap, and the carrier thread is freed. The developer sees none of this. It's invisible.

---

## Slide 23: Runtime Stack Manipulation

```java
Continuation cont = new Continuation(SCOPE, () -> {
    System.out.println("before");
    Continuation.yield(SCOPE);
    System.out.println("after");
});

cont.run();       // prints "before"
cont.isDone();    // false
cont.run();       // prints "after"
cont.isDone();    // true
```

- `yield()` **copies stack frames to the heap** — `run()` restores them
- The JVM manipulates the **actual thread stack** at runtime

| Ingredient     | Java (Runtime)                              |
|----------------|---------------------------------------------|
| Continuation   | `Continuation` class (JDK internal)         |
| Thread Pool    | `ForkJoinPool` (carrier threads)            |
| Scheduler      | JVM runtime scheduler                       |
| Yield/Suspend  | `Continuation.yield()` — stack to heap      |

- **Suspension points:** every blocking JDK call (`Thread.sleep`, `Socket.read`, `Lock.lock`) — **completely transparent** to the developer

> **Speaker notes:** Here's the magic. yield() copies stack frames to the heap, run() restores them. And the crucial difference: the developer never calls yield() directly. Every blocking call in the JDK — sleep, socket read, lock acquisition — internally contains a Continuation.yield(). The suspension points are invisible. No coloring, no Async, no suspend keyword. The JVM handles it for you. That's why virtual threads work with existing code without any changes.

---

## Slide 24: Stack Frames — From Thread to Heap and Back

```java
// Inside a class
void foo() { bar(); }       // ← pushes frame
void bar() {
    Continuation.yield(SCOPE);  // ← triggers copy
    System.out.println("resumed!");
}
Continuation cont = new Continuation(SCOPE, () -> {
    foo();       // ← pushes frame
});
cont.run();       // ← runs until yield (stack → heap)
cont.run();       // ← resumes at yield (heap → stack)
```

```
┌──── cont.run() ─── RUNNING on Carrier Thread 1 ────────────────┐
│                                                                │
│  Carrier Thread 1 Stack           Heap (Continuation object)   │
│  ┌─────────────────────┐          ┌─────────────────────┐      │
│  │ bar()  [line 2]     │          │                     │      │
│  │ foo()               │          │     (empty)         │      │
│  │ cont.lambda         │          │                     │      │
│  │ Continuation.run()  │          └─────────────────────┘      │
│  └─────────────────────┘                                       │
└────────────────────────────────────────────────────────────────┘

              │  bar() calls Continuation.yield(SCOPE)
              ▼

┌──── yield() ─── SUSPENDED ──────────────────────────────────────┐
│                                                                 │
│  Carrier Thread 1 Stack           Heap (Continuation object)    │
│  ┌─────────────────────┐   copy   ┌─────────────────────┐       │
│  │                     │ ──────►  │ bar()  [line 2]     │       │
│  │                     │   to     │ foo()               │       │
│  │     (free!)         │  heap    │ cont.lambda         │       │
│  │                     │          └─────────────────────┘       │
│  └─────────────────────┘    Stack frames are frozen on the heap │
│                             Carrier Thread 1 is FREE            │
└─────────────────────────────────────────────────────────────────┘

              │  cont.run() called again (possibly on a DIFFERENT carrier)
              ▼

┌──── cont.run() ─── RESUMED on Carrier Thread 3 ────────────────┐
│                                                                │
│  Carrier Thread 3 Stack           Heap (Continuation object)   │
│  ┌─────────────────────┐  copy    ┌─────────────────────┐      │
│  │ bar()  [line 2]     │ ◄──────  │                     │      │
│  │ foo()               │  from    │     (empty)         │      │
│  │ cont.lambda         │  heap    │                     │      │
│  │ Continuation.run()  │          └─────────────────────┘      │
│  └─────────────────────┘                                       │
│  bar() resumes at line 2 → prints "resumed!"                   │
└────────────────────────────────────────────────────────────────┘
```

- On `yield()`: the JVM **copies the virtual thread's stack frames** from the carrier thread stack **to the heap** (inside the `Continuation` object)
- On `run()`: the JVM **restores those frames** from the heap **onto a carrier thread** — possibly a **different** one
- The carrier thread is **never blocked** — it's immediately available for other virtual threads
- This is **O(stack depth)**, not O(full thread stack) — only the frames that belong to the continuation are copied

> **Speaker notes:** Let's look at what actually happens in memory. We have a continuation that calls foo, which calls bar, which yields. When bar calls yield, the JVM copies three stack frames — bar, foo, and the lambda — from the carrier thread's stack to the heap. The carrier thread is now free to run other virtual threads. Later, when we call run() again, the JVM copies those frames BACK onto a carrier thread's stack — and it might be a completely different carrier thread! Bar resumes exactly at the line after yield and prints "resumed!". This is how virtual threads achieve lightweight context switching: instead of an OS-level save-and-restore of the entire thread, the JVM copies just the relevant frames. And the cost is proportional to the stack depth, not to a fixed thread stack size.

---

## Slide 25: Not All Continuations Are the Same

- **Delimited** vs. **Undelimited**
  - **Undelimited**: captures the **entire** rest of the program — rarely useful in practice
  - **Delimited**: captures up to a **delimiter** (a boundary) — this is what we want

```
Undelimited:  [─── captured ──────────────────────────── ∞ ]
Delimited:    [─── captured ───── | delimiter ]   rest of program continues
```

- **One-shot** vs. **Multi-shot**
  - **One-shot**: a continuation can be resumed **exactly once** — then it's consumed
  - **Multi-shot**: a continuation can be **duplicated** and resumed **multiple times** (fork!)

- All three use **delimited** continuations — but they differ on **one-shot vs. multi-shot**

| | Scala | Kotlin | Java |
|---|---|---|---|
| **Delimiter** | The `Async` boundary (run loop stops) | The coroutine scope | `ContinuationScope` |
| **Resume** | `cb(Right(value))` | `resumeWith()` | `cont.run()` |
| **Multi-shot?** | **Yes** — `IO` is data, re-interpretable | No (throws `IllegalStateException`) | No (mutable internal state) |

- Scala's `IO` is an **ADT** — the `FlatMap` continuations are **plain functions**, reusable as values
- The same `IO[A]` tree can be interpreted **multiple times**, each producing a fresh execution
- Kotlin and Java continuations carry **mutable state** — they're consumed on resume

> **Speaker notes:** Before we wrap up, let's be precise about what KIND of continuations these are. There are two axes. First: delimited vs. undelimited. An undelimited continuation captures the entire rest of the program — that's too powerful and rarely useful. All three languages use DELIMITED continuations — they capture up to a boundary. In Scala, the delimiter is the Async boundary where the run loop stops. In Kotlin, it's the coroutine scope. In Java, it's the explicit ContinuationScope passed to yield(). Second axis: one-shot vs. multi-shot. Here's where they diverge. Scala's continuations are multi-shot — because IO is just data. The FlatMap chain stores plain functions as continuations. You can interpret the same IO tree multiple times, each run creating a fresh execution. The continuation is a VALUE you can reuse. Kotlin is strictly one-shot — calling resumeWith twice throws IllegalStateException. Java is also one-shot — the Continuation object has mutable internal state that tracks progress. This is a real practical difference: Scala's approach enables retry, memoization, and re-execution for free — you just run the same IO value again.

---

## Slide 26: The Concurrency Triangle

```
                    CONTINUATIONS
                         ▲
                        / \
                       /   \
                      /     \
        User Level   / SAME  \   Runtime Level
        (Library)   /  CORE   \  (JVM)
                   /  PATTERN  \
                  /             \
                 /               \
    Scala Fibers ─────────────── Java Virtual Threads
    IO ADT +                     VirtualThread +
    FlatMap as cont.             Continuation.yield()
                 \               /
                  \             /
                   \           /
                    \         /
                     \       /
                      \     /
                    Kotlin Coroutines
                    Compiler-generated
                    state machine + CPS

                    Compile Level
```

Three languages, three abstraction levels, **one pattern**.

| | Scala | Kotlin | Java |
|---|---|---|---|
| **Suspension points** | `Async` boundaries | `suspend` function calls | Blocking JDK calls |
| **Visibility** | Explicit | Compiler-enforced | Transparent |

> **Speaker notes:** And here's the triangle. Same four ingredients, three abstraction levels. But look at where suspension happens — that's the real difference. In Scala, YOU decide: every Async boundary is a suspension point, and you see it explicitly. In Kotlin, the COMPILER decides: every call to a suspend function is a potential suspension point, enforced through function coloring. In Java, the JVM decides: every blocking call in the JDK internally yields, completely transparent to the developer. From explicit to invisible. Three expressions of the same breakthrough — but with very different trade-offs in control versus convenience.

---

## Slide 27: References

- [How do Fibers Work? A Peek Under the Hood](https://www.youtube.com/watch?v=x5_MmZVLiSM)
- [Concurrency In Scala with Cats-Effect](https://github.com/slouc/concurrency-in-scala-with-ce)
- [Threading best practices in Cats Effect](https://timwspence.github.io/blog/posts/2021-01-12-threading-best-practices-cats-effect.html)
- [Cats Effect 3](https://www.youtube.com/watch?v=JrpFFRdf7Q8)
- [Core Runtimes > Schedulers](https://typelevel.org/cats-effect/docs/schedulers)
- [Kotlin 101: Coroutines Quickly Explained](https://rockthejvm.com/articles/kotlin-101-coroutines)
- [Kotlin Coroutine Internals](https://medium.com/better-programming/kotlin-coroutine-internals-49518ecf2977)
- [Coroutines under the hood](https://kt.academy/article/cc-under-the-hood)
- [The Ultimate Guide to Java Virtual Threads](https://rockthejvm.com/articles/the-ultimate-guide-to-java-virtual-threads)
- [Continuations - Under the Covers](https://www.youtube.com/watch?v=6nRS6UiN7X0)
- [Continuations: The magic behind virtual threads in Java](https://www.youtube.com/watch?v=HQsYsUac51g)

---

## Slide 28: Thank You

**Riccardo Cardin**

Scalar 2026

---

## Appendix: Slide-by-Slide Timing Guide (20 minutes)

| Slides  | Section           | Time     |
|---------|-------------------|----------|
| 1-2     | Title + Agenda    | ~1 min   |
| 3-6     | The Problem       | ~3 min   |
| 7-15    | Scala Fibers      | ~8 min   |
| 16-20   | Kotlin Coroutines | ~4 min   |
| 21-24   | Java VT           | ~3 min   |
| 25-26   | Comparison + Triangle | ~2 min |
| 27-28   | References + End  | ~0 min   |
