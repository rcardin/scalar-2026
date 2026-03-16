package in.rcard.scalar

import scala.collection.mutable.Stack
import java.util.concurrent.CountDownLatch

class IOFiber[A](io: IO[A], scheduler: Scheduler):
  private var currentIO: IO[Any] = io
  private val continuations       = Stack[Any => IO[Any]]()

  @volatile private var result: Option[Either[Throwable, Any]] = None
  private val latch = CountDownLatch(1)

  def run(): Unit =
    currentIO match
      case IO.Pure(value) =>
        if continuations.nonEmpty then
          currentIO = continuations.pop()(value)
          scheduler.submit(this)
        else
          result = Some(Right(value))
          latch.countDown()

      case IO.Delay(thunk) =>
        try
          currentIO = IO.Pure(thunk())
          scheduler.submit(this)
        catch
          case e: Throwable =>
            result = Some(Left(e))
            latch.countDown()

      case IO.FlatMap(inner, cont) =>
        continuations.push(cont.asInstanceOf[Any => IO[Any]])
        currentIO = inner
        scheduler.submit(this)

      case IO.Async(register) =>
        register {
          case Right(value) =>
            currentIO = IO.Pure(value)
            scheduler.submit(this)
          case Left(error) =>
            result = Some(Left(error))
            latch.countDown()
        }

  /** Produces an IO that suspends the current fiber until this fiber completes. */
  def join: IO[A] =
    IO.Async { cb =>
      // Spin up a daemon thread to wait, so we don't block a pool thread
      val waiter = new Thread(() => {
        latch.await()
        result match
          case Some(Right(v)) => cb(Right(v.asInstanceOf[A]))
          case Some(Left(e))  => cb(Left(e))
          case None           => cb(Left(new RuntimeException("Fiber completed without result")))
      })
      waiter.setDaemon(true)
      waiter.start()
    }
