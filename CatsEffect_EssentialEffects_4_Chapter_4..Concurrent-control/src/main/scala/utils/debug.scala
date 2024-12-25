package utils

import cats.effect._

object debug {
  /** Extension methods for an effect of type `IO[A]`. */
  implicit class DebugHelper[A](ioa: IO[A]) {
    /** Print to the console the value of the effect
     * along with the thread it was computed on. */
    def debugio: IO[A] =
      for {
        a <- ioa
        tn = Thread.currentThread.getName
        _ = println(s"[${Colorize.reversed(tn)}] $a")
      } yield a
  }
}
