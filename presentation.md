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
// Th4e continuation is implicit (it's the next line)
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

## Slide 8: IO as a Data Structure

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
- `FlatMap` — a computation followed by **a continuation**

> **Speaker notes:** Here's where Scala shines. In the effect system approach, we don't run code immediately. We build a data structure that DESCRIBES the computation. Look at FlatMap: it stores an IO and a function `A => IO[B]`. That function IS the continuation — it's literally "what to do next with the result".

---

## Slide 9: FlatMap IS a Continuation

```scala
extension [A](io: IO[A])
  def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(io, f)
  def map[B](f: A => B): IO[B] = FlatMap(io, a => Pure(f(a)))

object IO:
  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)
```

- Every call to `flatMap` **stores a continuation** in the data structure
- A program is a **chain of continuations**

```scala
val morningRoutine: IO[Unit] =
  IO.delay(println("Going to the bathroom"))
    .flatMap(_ => IO.delay(sleep(500)))       // continuation 1
    .flatMap(_ => IO.delay(println("Done!"))) // continuation 2
```

```
FlatMap
├── FlatMap
│   ├── Delay(() => println("Going to the bathroom"))
│   └── cont₁: _ => Delay(() => sleep(500))
└── cont₂: _ => Delay(() => println("Done!"))
```

> **Speaker notes:** Look at this. When we write flatMap, we're not executing anything. We're building a tree. Each flatMap node stores the continuation — the function that takes the previous result and produces the next computation. This is CPS made explicit as a data structure. The program IS a chain of continuations.

---

## Slide 10: The Run Loop — Interpreting Continuations

- The runtime **interprets** the IO tree, using a **trampoline**

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
- This is the key: we moved the execution state from the thread stack to a data structure **we control**

> **Speaker notes:** Here's the interpreter. Instead of using recursive calls (which eat thread stack), we use a while loop and an explicit stack of continuations on the heap. When we hit a FlatMap, we push the continuation onto our stack and process the inner IO. When we get a Pure value, we pop the next continuation and keep going. This is called trampolining — and it's the key trick. Our continuation stack is on the HEAP, not on the thread stack. We control it. We can pause it, resume it, move it to another thread. That's what makes fibers possible.

---

## Slide 11: Adding Suspension — The Async Case

- To **suspend** a fiber, we need a new IO case

```scala
enum IO[+A]:
  case Pure(value: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](io: IO[A], cont: A => IO[B]) extends IO[B]
  case Async(register: (Either[Throwable, A] => Unit) => Unit)
```

- `Async` captures a callback-based computation
- When the run loop hits `Async`, it **suspends** the fiber
  - The continuation stack is saved
  - The thread is **freed** to run other fibers
- When the callback fires, the fiber is **resumed**

```scala
def sleep(millis: Long): IO[Unit] =
  IO.Async { callback =>
    scheduler.schedule(
      () => callback(Right(())),
      millis,
      TimeUnit.MILLISECONDS
    )
  }
```

> **Speaker notes:** But our IO can only run synchronously so far. To actually yield a thread, we need Async. When the run loop hits an Async, it registers the callback and STOPS executing. The fiber is parked. The thread is free to pick up another fiber. When the callback eventually fires — say a timer, or an I/O completion — the fiber gets rescheduled. This is the suspension mechanism.

---

## Slide 12: Fibers — Lightweight Threads

- A **Fiber** wraps an IO with its own continuation stack
- Fibers are **cheap** — just objects on the heap

```scala
class IOFiber[A](io: IO[A], scheduler: Scheduler):
  private var currentIO: IO[Any] = io
  private val continuations = Stack[Any => IO[Any]]()

  def run(): Unit =
    currentIO match
      case Pure(value) =>
        if continuations.nonEmpty then
          currentIO = continuations.pop()(value)
          scheduler.submit(this) // Reschedule, don't recurse
        // else: fiber completed
      case Delay(thunk) =>
        currentIO = Pure(thunk())
        scheduler.submit(this)
      case FlatMap(inner, cont) =>
        continuations.push(cont)
        currentIO = inner
        scheduler.submit(this)
      case Async(register) =>
        register {
          case Right(value) =>
            currentIO = Pure(value)
            scheduler.submit(this) // Resume the fiber
          case Left(error) => /* handle error */
        }
```

> **Speaker notes:** A Fiber is simply our IO plus a continuation stack. Notice something crucial: after each step, the fiber re-submits itself to the scheduler instead of looping. This is cooperative scheduling — the fiber voluntarily yields control after each step, giving other fibers a chance to run. And when it hits Async, it truly suspends — no thread is held.

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
| Continuation   | `FlatMap(io, f)` — stored in the IO ADT     |
| Thread Pool    | `ExecutorService` (e.g., work-stealing pool)|
| Scheduler      | Custom run loop + fiber queue               |
| Yield/Suspend  | `Async` case — callback-based suspension    |

- Libraries like **Cats Effect** and **ZIO** implement this pattern with battle-tested optimizations
  - Work-stealing schedulers, auto-yielding, cancellation, structured concurrency
- Other libraries, like **Ox** and **YAES**, explore alternative approaches built on virtual threads and algebraic effects

> **Speaker notes:** So here's the recap for Scala. The continuation is the flatMap function stored in the IO tree. The thread pool runs fibers. The scheduler is a custom run loop. And suspension happens through Async callbacks. This is exactly what Cats Effect and ZIO do under the hood — with years of optimizations on top. Worth mentioning: Ox takes a different path, leveraging Java's virtual threads directly, and YAES explores algebraic effects. The Scala ecosystem gives you choices.

---

## Slide 16: Section Divider — Kotlin Coroutines

### 02

**Kotlin Coroutines**
Continuations at Compile Time

---

## Slide 17: The Compiler Does It For You

- In Kotlin, you write a **`suspend` function** — the compiler does the rest
- The compiler transforms it into a **state machine** with an explicit **continuation**

```kotlin
// What you write
suspend fun bathTime() {
  logger.info("Going to the bathroom")
  delay(500L)
  logger.info("Done with the bath")
}
```

```kotlin
// What the compiler generates
fun bathTime(callerContinuation: Continuation<*>): Any {
  val continuation = callerContinuation as? BathTimeContinuation
    ?: BathTimeContinuation(callerContinuation)
  if (continuation.label == 0) {
    logger.info("Going to the bathroom")
    continuation.label = 1
    if (delay(500L, continuation) == COROUTINE_SUSPENDED)
      return COROUTINE_SUSPENDED
  }
  if (continuation.label == 1) {
    logger.info("Done with the bath")
  }
}
```

> **Speaker notes:** Kotlin takes a completely different approach. You just mark a function as `suspend`, and the compiler rewrites it. It adds a Continuation parameter — the continuation of the CALLER. It transforms the body into a state machine where each suspension point is a label. When delay suspends, the function returns COROUTINE_SUSPENDED and the thread is free. When the delay completes, the continuation's `resumeWith` is called, and the function re-enters at label 1. Same pattern — but the compiler builds the continuation for you.

---

## Slide 18: Same Ingredients, Compiler-Generated

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

- The **continuation** is the `Continuation` object generated by the compiler
- `resumeWith` is how a suspending function **resumes its caller** — this is CPS
- The **Dispatcher** is the scheduler — it decides which thread runs the continuation
- **Coloring problem:** `suspend` is contagious — every caller must also be `suspend`

> **Speaker notes:** Same four ingredients. The Continuation is a compiler-generated object with resumeWith — that's CPS. The Dispatcher is the scheduler. And suspension is signaled by returning COROUTINE_SUSPENDED. The trade-off? Function coloring. You must mark every suspendable function with `suspend`, and it propagates up the call chain. In Scala, the IO monad has a similar coloring — everything returns IO. But in Kotlin it's enforced by the type system at compile time.

---

## Slide 19: Section Divider — Java Virtual Threads

### 03

**Java Virtual Threads**
Continuations at Runtime

---

## Slide 20: The JVM Does It For You

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

## Slide 21: Runtime Stack Manipulation

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

- `Continuation.yield()` **copies the stack frames** to the heap
- The next call to `run()` **restores them** and resumes execution
- The JVM manipulates the **actual thread stack** at runtime

| Ingredient     | Java (Runtime)                              |
|----------------|---------------------------------------------|
| Continuation   | `Continuation` class (JDK internal)         |
| Thread Pool    | `ForkJoinPool` (carrier threads)            |
| Scheduler      | JVM runtime scheduler                       |
| Yield/Suspend  | `Continuation.yield()` — stack to heap      |

> **Speaker notes:** Here's the magic. When you call Continuation.yield(), the JVM literally copies the stack frames from the thread stack to the heap. The thread is freed. When run() is called again, the frames are restored and execution continues from exactly where it left off. yield() returns! This is something neither Scala nor Kotlin can do — it requires JVM-level support. Every blocking call in the JDK — sleep, socket read, lock acquisition — now contains a Continuation.yield() call internally. That's why virtual threads work with existing code.

---

## Slide 22: The Concurrency Triangle

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

Three languages, three abstraction levels, **one pattern**: replacing OS thread scheduling with user-space scheduling of continuations.

> **Speaker notes:** And here's the triangle. Scala makes the continuation explicit — you can see it, touch it, it's the flatMap function in your IO data structure. Kotlin hides it at compile time — the compiler generates the state machine and the Continuation object. Java hides it at runtime — the JVM manipulates the stack directly. But strip away the surface differences and you'll find the same four ingredients: a continuation, a thread pool, a scheduler, and a way to yield. Three different expressions of the same fundamental breakthrough in concurrent programming.

---

## Slide 23: References

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

## Slide 24: Thank You

**Riccardo Cardin**

Scalar 2026

---

## Appendix: Slide-by-Slide Timing Guide (20 minutes)

| Slides  | Section           | Time     |
|---------|-------------------|----------|
| 1-2     | Title + Agenda    | ~1 min   |
| 3-6     | The Problem       | ~3 min   |
| 7-15    | Scala Fibers      | ~8 min   |
| 16-18   | Kotlin Coroutines | ~4 min   |
| 19-21   | Java VT           | ~3 min   |
| 22      | The Triangle      | ~1 min   |
| 23-24   | References + End  | ~0 min   |
