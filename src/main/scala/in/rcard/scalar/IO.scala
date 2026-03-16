package in.rcard.scalar

import java.util.concurrent.TimeUnit

enum IO[+A]:
  case Pure(value: A)
  case Delay(thunk: () => A)
  case FlatMap[A, B](io: IO[A], cont: A => IO[B]) extends IO[B]
  case Async(register: (Either[Throwable, A] => Unit) => Unit)

extension [A](io: IO[A])
  def flatMap[B](f: A => IO[B]): IO[B] = IO.FlatMap(io, f)
  def map[B](f: A => B): IO[B]         = IO.FlatMap(io, a => IO.Pure(f(a)))
  def start(using scheduler: Scheduler): IO[IOFiber[A]] =
    IO.Delay { () =>
      val fiber = IOFiber(io, scheduler)
      scheduler.submit(fiber)
      fiber
    }

object IO:
  def delay[A](thunk: => A): IO[A] = Delay(() => thunk)

  def sleep(millis: Long): IO[Unit] =
    IO.Async { cb =>
      Scheduler.timer.schedule(
        new java.util.TimerTask {
          def run(): Unit = cb(Right(()))
        },
        millis
      )
    }

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

        case Async(_) =>
          throw new UnsupportedOperationException(
            "Async is not supported in unsafeRun — use IOFiber with a Scheduler instead"
          )

    throw new RuntimeException("Unreachable")
