package in.rcard.scalar

object Main:

  def main(args: Array[String]): Unit =
    given scheduler: Scheduler = Scheduler(poolSize = 2)

    val bathTime: IO[Unit] =
      IO.delay(println(s"[${Thread.currentThread().getName}] Going to the bathroom"))
        .flatMap(_ => IO.sleep(500))
        .flatMap(_ => IO.delay(println(s"[${Thread.currentThread().getName}] Done with the bath")))

    val boilingWater: IO[Unit] =
      IO.delay(println(s"[${Thread.currentThread().getName}] Boiling some water"))
        .flatMap(_ => IO.sleep(1000))
        .flatMap(_ => IO.delay(println(s"[${Thread.currentThread().getName}] Water is ready")))

    val morningRoutine: IO[Unit] =
      for
        fiberA <- bathTime.start
        fiberB <- boilingWater.start
        _      <- fiberA.join
        _      <- fiberB.join
      yield ()

    println("=== Morning Routine (concurrent fibers on 2 threads) ===")
    val start = System.currentTimeMillis()

    // Bootstrap: run the morningRoutine IO on a fiber
    val mainFiber = IOFiber(morningRoutine, scheduler)
    scheduler.submit(mainFiber)

    // Wait for completion (block the main thread)
    mainFiber.join match
      case IO.Async(register) =>
        val latch = java.util.concurrent.CountDownLatch(1)
        register {
          case Right(_) => latch.countDown()
          case Left(e)  => e.printStackTrace(); latch.countDown()
        }
        latch.await()
      case _ => ()

    val elapsed = System.currentTimeMillis() - start
    println(s"=== Done in ${elapsed}ms (bath=500ms + water=1000ms ran concurrently) ===")
    System.exit(0)
