# The Concurrency Triangle — Discourse

## Opening (Slide 1 — Title)

Today I want to show you something funny. Scala, Kotlin, and Java — three very different worlds — they all solved concurrency in the same way. Same pattern. Same pieces. They just put them in different places. And by the end of this talk, you'll see it everywhere.

My name is Riccardo Cardin, and this is The Concurrency Triangle.

## Agenda (Slide 2)

So here's the plan. First, we'll look at the problem — why OS threads don't scale. Then we'll see how Scala fixes it with fibers, how Kotlin fixes it with coroutines, and how Java fixes it with virtual threads. And then we put them side by side and... surprise.

## Who Am I (Slide 3)

Quick intro — I've been doing Scala since 2011, so about fifteen years now. I also built YAES — Yet Another Effect System — a small effect system for Scala. The QR codes on screen go to my GitHub, LinkedIn, and blog if you want to connect.

Ok, let's go.

## Concurrency vs. Parallelism (Slide 4)

Before we start fixing things, let's make sure we agree on what we're fixing.

Concurrency and parallelism — not the same thing.

Concurrency is about the problem. You have many tasks, and they can run in any order. You can do concurrency with just one core — one executor juggling tasks, switching between them. Look at the diagram: Task 1 and Task 2 take turns. One executor. That's concurrency.

Parallelism is about the machine. You have enough CPUs to run things at the same time. Two executors, running together.

Today we're talking about concurrency. Many tasks, few threads. The question is: who decides what runs next, and how much does it cost?

## Threads Block (Slide 5)

Ok, so here's where things break.

When a thread calls a database, or makes an HTTP request, it blocks. The whole thread just sits there. The OS steps in — it saves everything, all the registers, all the state — and loads another thread. That's a context switch.

And context switches are not cheap. The OS has to go through the kernel, it trashes the CPU caches, it moves kilobytes of data around. And the worst part? The OS has no idea what our program wants to do next. So it saves everything. Just in case.

Quick question for the room — how many of you have seen thread exhaustion in production? That moment when your service stops answering because all threads are just... waiting?

Yeah. That's what we're fixing today. The OS manages the scheduling, but the OS doesn't know anything about our program.

## What If We Manage the Scheduling? (Slide 6)

So here's the idea. The OS doesn't know what our tasks need. But we do.

When a task is waiting for I/O, we know exactly what should happen next. It's the next line of code. The callback. In fancy words: the continuation.

Look at the code. In normal code, the continuation is just the next line — you don't think about it. In continuation-passing style, you make it visible: "when this finishes, call that function."

So the trick is: when a task blocks, save the continuation — just that small piece — free the thread, and pick it up later on any thread.

That's the idea behind fibers. Behind coroutines. Behind virtual threads. Same idea, three times.

## What We Need (Slide 7)

Every solution we'll see today needs four things. Just four.

First: what is a continuation? How do we represent it? Second: a thread pool to run them — those are the workers, the actual threads that execute code. Third: a scheduler to pick the next one — that's the decision-maker, the logic that says "you go next." They're related but not the same thing: the thread pool is the "where," the scheduler is the "who's next." Fourth: a way to pause — to yield, to suspend, to say "I'm done for now, come back later."

Keep these four in your head. We'll see them in Scala. Then in Kotlin. Then in Java. Every time, the same four things.

Same recipe, different kitchens.

## Scala Fibers — Divider (Slide 8)

Let's start with Scala. We're at Scalar, so this is home.

In Scala, the continuation lives in regular objects — FlatMap chains and Async callbacks. Nothing magic. Just data structures and functions that we build ourselves.

## IO — Describing a Program as Data (Slide 9)

Scala's way is: don't run the computation right away. First, describe it. Build a tree of instructions.

Look at the IO enum. Three cases. `Pure` — a value that's already done. `Delay` — a lazy thunk, something to run later. And `FlatMap` — this is the important one. It holds an `inner` IO and a function called `cont`: "when you get the result of the inner IO, call cont to decide what to do next." That `cont` function? That's a continuation.

Why describe instead of run? Because if it's data, we own it. We can decide when to run it, where to run it, how to run it.

## FlatMap Chains (Slide 10)

So we build chains. Look at `bathTime`: print, then sleep, then print again. Each `flatMap` adds a step. You can see the tree on screen.

But here's the thing — this is still sequential. If step two does `Thread.sleep`, the thread blocks. We have continuations in the data structure, sure, but we're not using them to free the thread yet. We have the pieces, but something is missing.

## The Run Loop (Slide 11)

Two problems to solve. First: recursive `flatMap` calls eat stack frames. Fix: replace recursion with a while loop and our own stack on the heap. `FlatMap`? Push `cont`, set `inner` as current. `Pure`? Pop the next continuation.

Now our execution state is a heap object we control. But the thread is still stuck in the loop — if something blocks, we can't free it. Heap stack: necessary, not sufficient. We still need a way to pause and release the thread.

## Async — The Real Continuation (Slide 12)

`Async` is the piece that solves blocking. Look at `sleep`: it hands a callback `cb` to a scheduler. When the timer fires, `cb` resumes the computation. That callback is the continuation.

Problem: `unsafeRun` is a while-true loop — it never releases the thread. We need to break execution into single steps, wrap the state in an object, and hand each step to a scheduler. That object is a Fiber.

## Fibers — Putting It All Together (Slide 13)

The `run` method has three branches. `Pure`: pop the next continuation, re-submit. `FlatMap`: push `cont`, re-submit. `Async`: register the callback — when it fires, re-submit.

Every step ends with re-submit. That's cooperative scheduling. At an `Async` boundary, the fiber suspends and releases the thread. When the callback fires, the fiber is re-submitted to the scheduler for execution. A fiber is just an object — you can have millions.

## The Scheduler (Slide 14)

The scheduler is simple. Really simple. A queue of fibers and a thread pool. Fiber yields? Back in the queue. Thread free? Pick the next fiber. Threads are always busy. No OS context switches. That's it.

## Morning Routine (Slide 15)

And here's the result. We start two fibers — `bathTime` and `boilingWater`. Each builds a chain of continuations. When they hit `sleep`, they suspend — thread is free. The scheduler runs something else. Timer fires, fiber wakes up. Two tasks, maybe one thread, zero context switches.

## Scala's Continuation Machinery (Slide 16)

Let me wrap up the Scala part. The continuation is the `Async` callback that wakes up a fiber's FlatMap chain. You, the programmer, choose where suspension happens. You see it, you control it.

Our toy version re-submits after every step. Real libraries — Cats Effect, ZIO, Kyo — they batch about 1024 steps before yielding. They use work-stealing pools. They handle cancellation. That's what makes them production-ready.

Now look at the table. Four rows. Continuation, thread pool, scheduler, yield. Remember these four rows. You're about to see them again.

## Kotlin Coroutines — Divider (Slide 17)

Now we cross the border to Kotlin. And this is where it gets fun — because Kotlin solves the same problem, but the continuation lives in a completely different place. Not in library objects. In a state machine that the compiler builds for you.

## The Compiler Rewrites Your Code (Slide 18)

In Kotlin, you write a `suspend` function. It looks like normal code. But the compiler changes it. It adds a `Continuation` parameter and splits the body at every suspension point.

Look at `bathTime`. Three lines. Looks simple. But the compiler turns it into something different — a state machine with a `Continuation` parameter.

Does this feel familiar? A function that takes a callback for "what to do next"? We just saw this five minutes ago in Scala.

## The State Machine (Slide 19)

Here's what the compiler actually makes. Look at the labels.

Label 0: run the first part, log, set label to 1, call `delay`. If delay says `COROUTINE_SUSPENDED` — return. Thread is free.

Label 1: when `delay` is done, it calls `sm.resumeWith()`. The function runs again with the same state machine. Label is 1 now, so we jump to "Done with the bath."

The `BathTimeSM` object holds the state between calls. It is the continuation. Same job as Scala's `Async` callback plus FlatMap chain. Just built by the compiler, not in a library at user level.

## resumeWith (Slide 20)

Now look at `suspendCoroutine`. This is a function from the Kotlin standard library — it's how you get access to the raw continuation object. You take it, hand it to a Timer, and when the timer fires, `resumeWith` picks up the coroutine.

Now look at the Scala code right below. `IO.Async` — you get a callback `cb`, you hand it to a Timer, and when the timer fires, you call `cb`. Same thing.

Both schedule a timer. Both resume via callback when the time is up. Different shape, same job. If you squint a little, `suspendCoroutine` is just Kotlin's version of `Async`.

## Kotlin Summary (Slide 21)

Same four things. Continuation is compiler-generated, with `resumeWith`. The Dispatcher is the scheduler. Suspension happens at every `suspend` call, and the compiler makes sure you can't miss it — that's function coloring.

There's the table again. Four rows. Same four rows. See the pattern?

## Java Virtual Threads — Divider (Slide 22)

Last one. Java. And Java goes one level deeper — into the JVM itself. No library objects. No compiler tricks. The runtime just does it.

## The JVM Does It For You (Slide 23)

Look at this code. `Thread.ofVirtual().start()`. That's all. No `IO`, no `suspend`, no `flatMap`. Just `Thread.sleep`. And it works.

Under the hood, `VirtualThread` wraps a `Continuation` object. A carrier thread is a real OS thread that runs the virtual thread. When the virtual thread calls `sleep` or reads from a socket, the JVM takes the stack frames, copies them to the heap, and frees the carrier.

Wait — "copies the stack to the heap"? We've heard this before. That's exactly what our Scala run loop does. Same idea, different level.

## Runtime Stack Manipulation (Slide 24)

Here's the raw `Continuation` API. Important: this is an internal JDK class — it's not part of the public API. You're not supposed to use it directly. But it's useful to see how it works.

Three calls: `run()` prints "before." `yield()` pauses — stack goes to the heap. Second `run()` resumes — prints "after."

The developer never calls `yield()` directly. Every blocking call in the JDK — `Thread.sleep`, `Socket.read`, `BlockingQueue.take` — calls `Continuation.yield()` inside. You never see it. That's the whole point.

## Stack Frames — Unmount (Slide 25)

The diagram shows it. On `yield`, the JVM takes the continuation's stack frames and moves them to the heap. Not the whole thread — just the continuation's frames. The carrier thread is now free.

## Stack Frames — Remount (Slide 26)

And on `run()`, those frames go back — onto any carrier thread, not necessarily the same one. `bar()` picks up right where it stopped. No context switch at all. Just copying frames back from the heap.

## Java Summary (Slide 27)

Same four things, last time. Continuation is a JDK-internal class. Thread pool is a `ForkJoinPool` of carriers. Scheduler is inside the JVM. And suspension? Totally invisible — every blocking JDK call yields the continuation for you.

There's the table. Four rows. Third time. Same four rows.

## The Concurrency Triangle (Slide 28)

And there it is. The triangle.

Three languages. Three levels. One pattern. Continuations on thread pools, with a scheduler and a way to yield.

Scala builds it in user space — you see everything, you control everything. Kotlin puts it in the compiler — you see the `suspend` keyword, the rest is hidden. Java puts it in the runtime — you see nothing at all.

Same idea. Three different trade-offs between control and convenience.

## Three Levels of Suspension (Slide 29)

Look at the table. Continuation, suspension, resume, visibility. Three columns, and every row is the same concept in different clothes.

Scala: you choose where to suspend. Kotlin: the compiler chooses. Java: the JVM chooses.

More control means more work. More transparency means less control. There's no winner here. It's a spectrum. Your project decides where you belong on it.

## One-Shot vs. Multi-Shot (Slide 30)

One more thing before we finish. In Scala, the IO is data. You can run the same IO as many times as you want — each time it creates fresh state. That's multi-shot. You get retry, re-execution, speculative execution for free.

Kotlin? Strictly one-shot. Call `resumeWith` twice and it throws. Java? Also one-shot — the continuation has mutable state that gets used up.

This is a real difference. Scala gives you something the others don't: your program is a value. You can re-run it.

## References (Slide 31)

The references are on screen. If you want to go deeper, the talks by Daniel Ciocîrlan and Ron Pressler are great starting points.

## Closing (Slide 32)

So here's what to take home. Next time someone tells you that fibers, coroutines, and virtual threads are completely different things — just smile, and draw them a triangle.

Four ingredients. One pattern. Three levels.

Thank you. Questions?
