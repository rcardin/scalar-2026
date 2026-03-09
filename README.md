# The Concurrency Triangle: Scala Fibers, Java Virtual Threads, and Kotlin Coroutines

Scala Fibers, Java Virtual Threads, and Kotlin Coroutines: this talk shows how this elegant solution manifests at three different abstraction levels.

## Abstract

Operating system thread context switches are a fundamental bottleneck in high-concurrency JVM applications. Each switch requires kernel transitions, CPU cache invalidation, and scheduling overhead. The JVM ecosystem has developed three prominent solutions: Fibers, as implemented by many Scala monadic effect systems; Java Virtual Threads (Project Loom); and Kotlin Coroutines.

While these technologies appear vastly different on the surface, functional versus imperative, library versus runtime, they actually implement the same core pattern: replacing OS thread scheduling with user-space scheduling of continuations.

This talk shows how this elegant solution manifests at three different abstraction levels. We'll take the Cats Effect library as an example of Fibers implementation library through the IO monad. Project Loom pushes it down to the JVM runtime, making it transparent to developers. In Scala, the Ox library is built upon virtual threads. Kotlin Coroutines place it at compile time by transforming suspend functions. We'll see them as different expressions of the same fundamental breakthrough in concurrent programming.

## References
 * [How do Fibers Work? A Peek Under the Hood](https://www.youtube.com/watch?v=x5_MmZVLiSM)
 * [Concurrency In Scala with Cats-Effect](https://github.com/slouc/concurrency-in-scala-with-ce?tab=readme-ov-file)
 * [Threading best practices in Cats Effect](https://timwspence.github.io/blog/posts/2021-01-12-threading-best-practices-cats-effect.html)
 * [Cats Effect 3](https://www.youtube.com/watch?v=JrpFFRdf7Q8)
 * [Core Runtimes > Schedulers](https://typelevel.org/cats-effect/docs/schedulers)
 * [Kotlin 101: Coroutines Quickly Explained](https://rockthejvm.com/articles/kotlin-101-coroutines)
 * [Kotlin Coroutine Internals](https://medium.com/better-programming/kotlin-coroutine-internals-49518ecf2977)
 * [Coroutines under the hood](https://kt.academy/article/cc-under-the-hood)
 * [The Ultimate Guide to Java Virtual Threads](https://rockthejvm.com/articles/the-ultimate-guide-to-java-virtual-threads#how-to-create-a-virtual-thread)
 * [Continuations - Under the Covers](https://www.youtube.com/watch?v=6nRS6UiN7X0)
 * [Continuations: The magic behind virtual threads in Java](https://www.youtube.com/watch?v=HQsYsUac51g)
