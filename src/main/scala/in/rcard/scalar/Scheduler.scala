package in.rcard.scalar

import java.util.concurrent.{ConcurrentLinkedQueue, ExecutorService, Executors}

class Scheduler(threadPool: ExecutorService):
  private val queue = ConcurrentLinkedQueue[IOFiber[?]]()

  def submit(fiber: IOFiber[?]): Unit =
    queue.add(fiber)
    threadPool.submit(new Runnable {
      def run(): Unit =
        val next = queue.poll()
        if next != null then next.run()
    })

object Scheduler:
  val timer = new java.util.Timer(true) // daemon timer for IO.sleep

  def apply(poolSize: Int): Scheduler =
    new Scheduler(Executors.newFixedThreadPool(poolSize))
