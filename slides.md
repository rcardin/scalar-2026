---
marp: true
theme: default
paginate: true
style: |
  /* --- Catppuccin Macchiato Palette --- */
  :root {
    --ctp-base: #24273a;
    --ctp-mantle: #1e2030;
    --ctp-crust: #181926;
    --ctp-surface0: #363a4f;
    --ctp-surface1: #494d64;
    --ctp-surface2: #5b6078;
    --ctp-overlay0: #6e738d;
    --ctp-text: #cad3f5;
    --ctp-subtext0: #a5adcb;
    --ctp-subtext1: #b8c0e0;
    --ctp-lavender: #b7bdf8;
    --ctp-blue: #8aadf4;
    --ctp-sapphire: #7dc4e4;
    --ctp-teal: #8bd5ca;
    --ctp-green: #a6da95;
    --ctp-yellow: #eed49f;
    --ctp-peach: #f5a97f;
    --ctp-red: #ed8796;
    --ctp-mauve: #c6a0f6;
    --ctp-pink: #f5bde6;
    --ctp-flamingo: #f0c6c6;
    --ctp-rosewater: #f4dbd6;
  }

  /* --- Global Section --- */
  section {
    background-color: var(--ctp-base);
    color: var(--ctp-text);
    font-family: 'Inter', 'SF Pro Display', 'Segoe UI', sans-serif;
    font-size: 28px;
  }

  /* --- Headings --- */
  section h1 {
    color: var(--ctp-mauve);
    border-bottom: 2px solid var(--ctp-surface1);
    padding-bottom: 8px;
  }
  section h2 {
    color: var(--ctp-lavender);
  }
  section h3 {
    color: var(--ctp-blue);
  }

  /* --- Strong / Bold --- */
  section strong {
    color: var(--ctp-peach);
  }

  /* --- Links --- */
  section a {
    color: var(--ctp-sapphire);
    text-decoration: underline;
    text-decoration-color: var(--ctp-surface2);
  }
  section a:hover {
    color: var(--ctp-blue);
  }

  /* --- Inline code --- */
  section code {
    background-color: var(--ctp-surface0);
    color: var(--ctp-green);
    border-radius: 4px;
    padding: 2px 6px;
    font-size: 22px;
    font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
  }

  /* --- Code blocks --- */
  section pre {
    background-color: var(--ctp-mantle) !important;
    border: 1px solid var(--ctp-surface0);
    border-radius: 8px;
    padding: 16px !important;
    font-size: 19px;
    line-height: 1.4;
  }
  section pre code {
    background-color: transparent;
    color: var(--ctp-text);
    padding: 0;
    font-size: 19px;
  }

  /* --- Syntax Highlighting (highlight.js overrides) --- */
  .hljs-string,
  .hljs-doctag,
  .hljs-regexp {
    color: var(--ctp-green) !important;
  }
  .hljs-keyword,
  .hljs-selector-tag,
  .hljs-type {
    color: var(--ctp-mauve) !important;
  }
  .hljs-built_in,
  .hljs-title.class_,
  .hljs-class .hljs-title {
    color: var(--ctp-yellow) !important;
  }
  .hljs-title.function_,
  .hljs-function .hljs-title {
    color: var(--ctp-blue) !important;
  }
  .hljs-number,
  .hljs-literal {
    color: var(--ctp-peach) !important;
  }
  .hljs-comment {
    color: var(--ctp-overlay0) !important;
    font-style: italic;
  }
  .hljs-variable,
  .hljs-template-variable {
    color: var(--ctp-text) !important;
  }
  .hljs-attr,
  .hljs-attribute {
    color: var(--ctp-yellow) !important;
  }
  .hljs-symbol,
  .hljs-bullet {
    color: var(--ctp-teal) !important;
  }
  .hljs-params {
    color: var(--ctp-text) !important;
  }
  .hljs-meta {
    color: var(--ctp-pink) !important;
  }
  .hljs-subst {
    color: var(--ctp-flamingo) !important;
  }

  /* --- Lists --- */
  section ul, section ol {
    color: var(--ctp-text);
  }
  section li::marker {
    color: var(--ctp-mauve);
  }

  /* --- Tables --- */
  section table {
    font-size: 22px;
    border-collapse: collapse;
    width: 100%;
  }
  section table th {
    background-color: var(--ctp-surface0);
    color: var(--ctp-mauve);
    border: 1px solid var(--ctp-surface1);
    padding: 8px 12px;
  }
  section table td {
    background-color: var(--ctp-mantle);
    color: var(--ctp-text);
    border: 1px solid var(--ctp-surface1);
    padding: 8px 12px;
  }

  /* --- Blockquote (used for callouts) --- */
  section blockquote {
    border-left: 4px solid var(--ctp-mauve);
    background-color: var(--ctp-surface0);
    padding: 8px 16px;
    border-radius: 4px;
    color: var(--ctp-subtext1);
  }

  /* --- Pagination --- */
  section::after {
    color: var(--ctp-overlay0);
    font-size: 16px;
  }

  /* --- Lead slides (title, thank you) --- */
  section.lead {
    background-color: var(--ctp-crust);
    text-align: center;
    justify-content: center;
  }
  section.lead h1 {
    color: var(--ctp-mauve);
    font-size: 56px;
    border-bottom: none;
  }
  section.lead h2 {
    color: var(--ctp-lavender);
    font-size: 30px;
    font-weight: 400;
  }
  section.lead p {
    color: var(--ctp-subtext0);
    font-size: 24px;
  }
  section.lead strong {
    color: var(--ctp-peach);
  }

  /* --- Section divider slides --- */
  section.divider {
    background: linear-gradient(135deg, var(--ctp-crust) 0%, var(--ctp-base) 100%);
    text-align: center;
    justify-content: center;
  }
  section.divider h2 {
    font-size: 20px;
    color: var(--ctp-overlay0);
    letter-spacing: 4px;
    text-transform: uppercase;
  }
  section.divider h1 {
    color: var(--ctp-mauve);
    font-size: 48px;
    border-bottom: none;
  }
  section.divider p {
    color: var(--ctp-subtext0);
    font-size: 24px;
  }
---

<!-- _class: lead -->
<!-- _paginate: false -->

# The Concurrency Triangle

## Scala Fibers, Java Virtual Threads, and Kotlin Coroutines

**Riccardo Cardin**
Scalar 2026

<!--
Welcome everyone. Today we're going to look at how three JVM languages solve the same fundamental problem — and spoiler: they all end up at the same place, just at different abstraction levels.
-->

---

# Agenda

1. The Problem
2. Scala Fibers — Continuations at the user level
3. Kotlin Coroutines — Continuations at compile time
4. Java Virtual Threads — Continuations at runtime
5. References

<!--
We'll start by understanding why OS threads are a bottleneck, then see how Scala, Kotlin, and Java each solve it. The key insight: all three implement the same pattern — continuations on thread pools. They just do it at different abstraction levels.
-->

---

# Concurrency vs. Parallelism

- **Concurrency** is a semantic feature **of a problem**
  - Many **tasks** that can be **interleaved**
  - We can have concurrency with a **single executor**

```
Task 1:     [1] [2] [3]
Task 2:     [a] [b] [c]
Execution:  [1] [2] [a] [3] [b] [c]   ← one executor, interleaved
```

- **Parallelism** is a property **of the runtime**
  - Enough **resources** to run tasks **at the same time**

```
Executor 1: [1] [2] [3]
Executor 2: [a] [b] [c]   ← two executors, truly simultaneous
```

- Today: **concurrency** — how to interleave many tasks on limited threads

<!--
Concurrency is about the PROBLEM — tasks that can run in any order. Parallelism is about the RUNTIME — enough CPUs to run them simultaneously. You can have concurrency without parallelism: one core juggling many tasks. What we're solving today is a concurrency problem — thousands of tasks, limited threads. Who manages the interleaving, and at what cost?
-->

---

# Threads Block — And That's the Problem

- When a thread calls I/O, it **blocks**
  - The **entire thread stack** is frozen
  - The OS **unmounts** the thread — another is **mounted**: **context switch**
- Context switching is **expensive**
  - Kernel transition, CPU cache invalidation, register save/restore
- The OS has **no idea** what our program wants to do next

```
Task A: [ work ][ wait for I/O... ][ work ]
Thread:  ───────┤                  ├───────
                ↓ OS context switch ↓
                 (save entire stack, (restore entire stack,
                  load another one)   resume from where
                                      we left off)
```

<!--
When a thread calls a database, it just sits there. The OS saves all the registers, the entire stack, and loads another thread. That's a context switch — kernel transition, invalidating CPU caches, moving kilobytes of data. The OS treats the thread as a black box: save everything, restore everything. That's the fundamental problem.
-->

---

# What If WE Manage the Scheduling?

- The OS doesn't know what our tasks need — but **we do**
- Key idea: save **what to do next** — a **continuation**

```scala
// The continuation is implicit (it's the next line)
val result = add(1, multiply(2, 3))
println(s"The result is: $result")

// Continuation-Passing Style — the continuation is explicit
multiplyCPS(2, 3) { multiplyResult =>
  addCPS(1, multiplyResult) { addResult =>
    println(s"The result is: $addResult")
  }
}
```

- **Suspend** a task: save its continuation, **free the thread**
- **Resume** it later: pick up the continuation — on **any** thread
- No OS context switch

<!--
The OS does a heavyweight operation because it doesn't know what our program needs. But WE know. When a task waits for I/O, we know exactly what should happen next — it's the next line, the callback, the "continuation." Save that tiny piece of information, free the thread. That's the idea behind fibers, coroutines, and virtual threads.
-->

---

# What We Need

A continuation-based runtime needs four things:

1. A definition of what a **continuation** is
2. A **thread pool** to run continuations
3. A **scheduler** to pick the next continuation to run
4. A way to **yield** or **suspend** a continuation

Let's see how **Scala**, **Kotlin**, and **Java** implement each of these.

<!--
Every solution we'll see today needs these four ingredients. The difference is WHERE each language implements them. Scala does it in user-space libraries. Kotlin does it at compile time. Java does it inside the JVM runtime. Same recipe, different kitchens.
-->

---

<!-- _class: divider -->

## 01

# Scala Fibers

Continuations at the User Level

---

# IO — Describing a Program as Data

- The `IO` monad is an **ADT** — a tree of instructions

```scala
enum IO[+A]:
  case Pure(value: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](io: IO[A], cont: A => IO[B]) extends IO[B]
```

- `Pure` — a completed value | `Delay` — a lazy thunk | `FlatMap` — **what to do next**

```scala
extension [A](io: IO[A])
  def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(io, f)
  def map[B](f: A => B): IO[B] = FlatMap(io, a => Pure(f(a)))

object IO:
  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)
```

<!--
In the effect system approach, we don't run code immediately. We build a data structure that DESCRIBES the computation. FlatMap stores an IO and a function A => IO[B] — "what to do next with the result." Each flatMap call adds a link to the chain. But this is just a description — it doesn't solve concurrency yet.
-->

---

# FlatMap Chains — Sequential, Not Yet Concurrent

```scala
val bathTime: IO[Unit] =
  IO.delay(println("Going to the bathroom"))
    .flatMap(_ => IO.delay(Thread.sleep(500)))  // still blocks!
    .flatMap(_ => IO.delay(println("Done!")))
```

```
FlatMap
├── FlatMap
│   ├── Delay(() => println("Going to the bathroom"))
│   └── step₂: _ => Delay(() => Thread.sleep(500))
└── step₃: _ => Delay(() => println("Done!"))
```

- Each `flatMap` stores "what to do next" — a **continuation**
- But this chain is **purely sequential**
- If step 2 blocks, the thread is **still blocked**
- We have continuations, but we're **not using them for concurrency yet**

<!--
Each flatMap stores a function — what to do next. That IS a continuation. But if we interpret this chain top to bottom, it's no different from sequential code. When we hit Thread.sleep, the thread blocks. We have the building blocks but need one more ingredient to suspend and resume.
-->

---

# The Run Loop — A Trampoline on the Heap

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

- The **stack of continuations** lives on the **heap**, not the thread stack
- We can **pause** it, **save** it, and **resume** it later

<!--
Instead of recursive calls that eat thread stack, we use a while loop and an explicit stack of continuations on the heap. Our execution state is now a regular object on the heap — we OWN it. We can pause this loop, save the stack, free the thread, and resume later. We just need a way to trigger suspension.
-->

---

# Async — The Real Continuation

- `Async` bridges **callback-based APIs** into the IO world

```scala
enum IO[+A]:
  // ... Pure, Delay, FlatMap ...
  case Async(register: (Either[Throwable, A] => Unit) => Unit)
```

```scala
def sleep(millis: Long): IO[Unit] =
  IO.Async { cb =>    // cb is the continuation!
    scheduler.schedule(() => cb(Right(())), millis, MILLISECONDS)
  }
```

- When the run loop hits `Async`:
  1. **Stops** the trampoline — continuation stack stays on the heap
  2. **Frees the thread**
  3. When `cb` fires, the fiber is **rescheduled**
- `FlatMap` builds the chain; `Async` is where we **cut it** and **resume later**

<!--
HERE is where the continuation pattern solves concurrency. Async is an FFI to the callback world. The callback cb IS the continuation. When the run loop hits Async, it hands cb to the external API and STOPS. The thread walks away. When the operation completes, cb re-submits the fiber with its full continuation stack. The fiber resumes exactly where it left off.
-->

---

# Fibers — Putting It All Together

```scala
class IOFiber[A](io: IO[A], scheduler: Scheduler):
  private var currentIO: IO[Any] = io
  private val continuations = Stack[Any => IO[Any]]()

  def run(): Unit = currentIO match
    case Pure(value) =>
      if continuations.nonEmpty then
        currentIO = continuations.pop()(value)
        scheduler.submit(this)         // Continue on the pool
    case Delay(thunk) =>
      currentIO = Pure(thunk())
      scheduler.submit(this)
    case FlatMap(inner, cont) =>
      continuations.push(cont)
      currentIO = inner
      scheduler.submit(this)
    case Async(register) =>
      register { case Right(value) =>  // Suspend! Free the thread
        currentIO = Pure(value)
        scheduler.submit(this)         // Resume when callback fires
        // ...
      }
```

- After each step: **re-submit** to scheduler — **cooperative scheduling**

<!--
A Fiber combines everything: the IO data structure, a continuation stack on the heap, and Async suspension. The run method re-submits the fiber to the scheduler after each step — that's cooperative scheduling. At Async, the thread walks away. The callback wakes it up later. A fiber is just an object — you can create millions.
-->

---

# The Scheduler — Cooperative Scheduling

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
- **Cooperative scheduling**: fibers yield voluntarily at `Async` boundaries
- No **context switch** at the OS level

<!--
The scheduler is surprisingly simple. A queue of fibers and a thread pool. When a fiber yields, it goes back in the queue. A thread picks up the next fiber. Threads are never idle. And because we manage scheduling in user space, we never pay OS context switch costs.
-->

---

# Putting It All Together — Morning Routine

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
    fiberA <- bathTime.start
    fiberB <- boilingWater.start
    _      <- fiberA.join
    _      <- fiberB.join
  yield ()
```

- `start` creates a new `IOFiber` and submits it to the scheduler
- Both tasks run **concurrently** — **no OS threads wasted**

<!--
We start two fibers. Each builds a chain of continuations via flatMap. When they hit sleep, they suspend — the thread is freed. The scheduler runs other fibers. When the timer fires, the fiber resumes. Two concurrent tasks, potentially on a single thread, zero context switches.
-->

---

# Scala's Continuation Machinery

| Ingredient     | Scala (User Level)                          |
|----------------|---------------------------------------------|
| Continuation   | `Async` callback + `FlatMap` chain          |
| Thread Pool    | `ExecutorService` (e.g., work-stealing pool)|
| Scheduler      | Custom run loop + fiber queue               |
| Yield/Suspend  | `Async` case — callback-based suspension    |

- **Suspension points:** every `Async` boundary — the programmer **explicitly** chooses
- **Cats Effect**, **ZIO** implement this with battle-tested optimizations
- **Ox**, **YAES** explore alternative approaches

<!--
The continuation is the Async callback that resumes a fiber's FlatMap chain. The programmer decides where suspension happens — every Async boundary. You see it, you control it. Cats Effect and ZIO do this under the hood. Ox and YAES take different paths. The Scala ecosystem gives you choices.
-->

---

<!-- _class: divider -->

## 02

# Kotlin Coroutines

Continuations at Compile Time

---

# The Compiler Rewrites Your Code

- You write a **`suspend` function** — the compiler transforms it

```kotlin
// What you write
suspend fun bathTime() {
  logger.info("Going to the bathroom")
  delay(500L)
  logger.info("Done with the bath")
}
```

- The compiler: adds a `Continuation` parameter, splits at every **suspension point**

```kotlin
// What the compiler generates (simplified)
fun bathTime(callerContinuation: Continuation<*>): Any {
  val sm = callerContinuation as? BathTimeSM
    ?: BathTimeSM(callerContinuation)
  if (sm.label == 0) { /* ... */ }
  if (sm.label == 1) { /* ... */ }
}
```

<!--
Kotlin takes a completely different approach. Mark a function as suspend, and the compiler rewrites it. It adds a Continuation parameter and splits the function body at every suspension point. Each segment becomes a branch in a state machine.
-->

---

# The State Machine — Step by Step

```kotlin
fun bathTime(callerContinuation: Continuation<*>): Any {
  val sm = callerContinuation as? BathTimeSM
    ?: BathTimeSM(callerContinuation)      // ① Create state machine
  if (sm.label == 0) {                      // ② First segment
    logger.info("Going to the bathroom")
    sm.label = 1                            // ③ Set resume point
    if (delay(500L, sm) == COROUTINE_SUSPENDED)
      return COROUTINE_SUSPENDED            // ④ Yield the thread!
  }
  if (sm.label == 1) {                      // ⑤ Resumed here
    logger.info("Done with the bath")
  }
}
```

- **First call** (label 0): runs segment 1, returns `COROUTINE_SUSPENDED` — thread is **free**
- **Second call** (label 1): `delay` calls `sm.resumeWith()`, re-enters at label 1
- The `BathTimeSM` object holds **state** between calls

<!--
First call: label is 0, we log, set label to 1, call delay. delay returns COROUTINE_SUSPENDED. The thread is free. Later, when the timer fires, delay calls sm.resumeWith(). bathTime is called AGAIN with the same state machine. Now label is 1, so we jump to "Done with the bath." The BathTimeSM object IS the continuation.
-->

---

# resumeWith — The Continuation in Action

- `suspendCoroutine` suspends and provides the `Continuation` — Kotlin's `Async`

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

- Both register a callback — when it fires, **resume the computation**

<!--
suspendCoroutine gives you the continuation object. You hand it to a Timer. When it fires, resumeWith resumes the coroutine. Now look at Scala's Async — same pattern. Register a callback, resume when done. Different shapes, same role.
-->

---

# Kotlin — Same Ingredients, Compiler-Generated

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
- **Suspension points:** every call to a `suspend` function — **compiler enforces** them
- **Coloring problem:** `suspend` is contagious

<!--
Same four ingredients. The Continuation is compiler-generated with resumeWith — that's CPS. The Dispatcher is the scheduler. The suspension points are every call to a suspend function. The compiler enforces this through function coloring — you MUST mark suspendable functions with suspend.
-->

---

<!-- _class: divider -->

## 03

# Java Virtual Threads

Continuations at Runtime

---

# The JVM Does It For You

- Reuse the `Thread` API with a different implementation

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
VirtualThread(..., Runnable task) {
    this.scheduler = scheduler;
    this.cont = new VThreadContinuation(this, task);
}
```

- **No coloring**, no special syntax, no monadic wrapping
- `Thread.sleep`, `Socket.read` — all **yield automatically**

<!--
Java: no new syntax, no new types. Just Thread.ofVirtual(). Under the hood, VirtualThread wraps a Continuation object. When the virtual thread calls sleep or does I/O, the JVM calls Continuation.yield(), copies the stack to the heap, and frees the carrier thread. Completely invisible to the developer.
-->

---

# Runtime Stack Manipulation

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

| Ingredient     | Java (Runtime)                              |
|----------------|---------------------------------------------|
| Continuation   | `Continuation` class (JDK internal)         |
| Thread Pool    | `ForkJoinPool` (carrier threads)            |
| Scheduler      | JVM runtime scheduler                       |
| Yield/Suspend  | `Continuation.yield()` — stack to heap      |

- **Suspension points:** every blocking JDK call — **completely transparent**

<!--
yield() copies stack frames to the heap, run() restores them. The developer never calls yield() directly. Every blocking call in the JDK internally contains a Continuation.yield(). No coloring, no Async, no suspend keyword. The JVM handles it for you.
-->

---

# Stack Frames — From Thread to Heap and Back

```
┌──── cont.run() ─── RUNNING on Carrier Thread 1 ──────────────┐
│  Carrier Thread 1 Stack       Heap (Continuation object)      │
│  ┌───────────────────┐        ┌───────────────────┐           │
│  │ bar()  [line 2]   │        │     (empty)       │           │
│  │ foo()             │        └───────────────────┘           │
│  │ cont.lambda       │                                        │
│  └───────────────────┘                                        │
└───────────────────────────────────────────────────────────────┘
          │  Continuation.yield(SCOPE)  →  stack copied to heap
          ▼
┌──── SUSPENDED ────────────────────────────────────────────────┐
│  Carrier Thread 1 Stack       Heap (Continuation object)      │
│  ┌───────────────────┐  copy  ┌───────────────────┐           │
│  │     (free!)       │ ────►  │ bar()  [line 2]   │           │
│  └───────────────────┘        │ foo()             │           │
│                               │ cont.lambda       │           │
│  Thread 1 is FREE             └───────────────────┘           │
└───────────────────────────────────────────────────────────────┘
          │  cont.run() again  →  heap copied to (any) carrier
          ▼
┌──── RESUMED on Carrier Thread 3 ──────────────────────────────┐
│  Carrier Thread 3 Stack       Heap (Continuation object)      │
│  ┌───────────────────┐  from  ┌───────────────────┐           │
│  │ bar()  [line 2]   │ ◄──── │     (empty)       │           │
│  │ foo()             │  heap  └───────────────────┘           │
│  │ cont.lambda       │                                        │
│  └───────────────────┘                                        │
│  bar() resumes → prints "resumed!"                            │
└───────────────────────────────────────────────────────────────┘
```

<!--
On yield, the JVM copies stack frames from the carrier thread to the heap. The carrier is free. On run(), frames are restored onto any carrier thread. The cost is O(stack depth), not O(full thread stack). Lightweight context switching without OS involvement.
-->

---

# Not All Continuations Are the Same

- **Delimited** vs. **Undelimited**
  - **Undelimited**: captures the **entire** rest of the program
  - **Delimited**: captures up to a **delimiter** (a boundary)

```
Undelimited:  [─── captured ──────────────────────────── ∞ ]
Delimited:    [─── captured ───── | delimiter ]   rest of program continues
```

- **One-shot** vs. **Multi-shot**
  - **One-shot**: resumed **exactly once** — then consumed
  - **Multi-shot**: can be **duplicated** and resumed **multiple times**

- All three are **delimited** — but they differ on **one-shot vs. multi-shot**

| | Scala | Kotlin | Java |
|---|---|---|---|
| **Delimiter** | `Async` boundary | Coroutine scope | `ContinuationScope` |
| **Resume** | `cb(Right(value))` | `resumeWith()` | `cont.run()` |
| **Multi-shot?** | **Yes** — `IO` is data | No (throws!) | No (mutable state) |

- Scala: `IO` is an ADT — `FlatMap` continuations are **plain functions**, re-interpretable
- Kotlin & Java: continuations carry **mutable state** — consumed on resume

<!--
Two axes. Delimited vs undelimited: all three use DELIMITED continuations — they capture up to a boundary. In Scala, the Async boundary. In Kotlin, the coroutine scope. In Java, the ContinuationScope. One-shot vs multi-shot: here they diverge. Scala's continuations are multi-shot — IO is just data. The FlatMap chain stores plain functions. You can interpret the same IO tree multiple times, each producing a fresh execution. Kotlin is strictly one-shot — resumeWith twice throws IllegalStateException. Java is also one-shot — mutable internal state. This is a real practical difference: Scala's approach enables retry and re-execution for free.
-->

---

# The Concurrency Triangle

```
                    CONTINUATIONS
                         ▲
                        / \
                       /   \
        User Level    / SAME \   Runtime Level
        (Library)    / CORE   \  (JVM)
                    / PATTERN  \
                   /            \
      Scala Fibers ──────────── Java Virtual Threads
      IO ADT +                   VirtualThread +
      FlatMap as cont.           Continuation.yield()
                   \            /
                    \          /
                  Kotlin Coroutines
                  Compiler-generated
                  state machine + CPS
                    Compile Level
```

Three languages, three abstraction levels, **one pattern**.

| | Scala | Kotlin | Java |
|---|---|---|---|
| **Suspension** | `Async` boundaries | `suspend` calls | Blocking JDK calls |
| **Visibility** | Explicit | Compiler-enforced | Transparent |

<!--
Same four ingredients, three abstraction levels. In Scala, YOU decide where suspension happens — every Async boundary. In Kotlin, the COMPILER decides — every suspend function call. In Java, the JVM decides — every blocking call, completely transparent. From explicit to invisible. Three expressions of the same breakthrough, with different trade-offs in control versus convenience.
-->

---

# References

- [How do Fibers Work? A Peek Under the Hood](https://www.youtube.com/watch?v=x5_MmZVLiSM)
- [Concurrency In Scala with Cats-Effect](https://github.com/slouc/concurrency-in-scala-with-ce)
- [Cats Effect 3](https://www.youtube.com/watch?v=JrpFFRdf7Q8)
- [Kotlin 101: Coroutines Quickly Explained](https://rockthejvm.com/articles/kotlin-101-coroutines)
- [Kotlin Coroutine Internals](https://medium.com/better-programming/kotlin-coroutine-internals-49518ecf2977)
- [Coroutines under the hood](https://kt.academy/article/cc-under-the-hood)
- [The Ultimate Guide to Java Virtual Threads](https://rockthejvm.com/articles/the-ultimate-guide-to-java-virtual-threads)
- [Continuations - Under the Covers](https://www.youtube.com/watch?v=6nRS6UiN7X0)
- [Continuations: The magic behind virtual threads](https://www.youtube.com/watch?v=HQsYsUac51g)

---

<!-- _class: lead -->
<!-- _paginate: false -->

# Thank You

**Riccardo Cardin**
Scalar 2026
