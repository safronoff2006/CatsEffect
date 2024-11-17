
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, IOApp}
import cats.effect.cps._

import java.lang.Thread.sleep
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


object HelloWorld extends IOApp.Simple {
  val run = IO.println("Cats Effect - Hello, World!")
}

object StupidFizzBuzz extends IOApp.Simple {
  val run =
    for {
      ctr <- IO.ref(0)

      wait = IO.sleep(1 second)
      poll = wait *> ctr.get

      _ <- poll.flatMap(IO.println(_)).foreverM.start
      _ <- poll.map(_ % 3 == 0).ifM(IO.println("fizz"), IO.unit).foreverM.start
      _ <- poll.map(_ % 5 == 0).ifM(IO.println("buzz"), IO.unit).foreverM.start

      _ <- (wait *> ctr.update(_ + 1)).foreverM.void
    } yield ()
}

object Main1 extends App {
  println("Изучаем Cats Effect")

  object part1 {
    def chapter1 = {
      val program = IO.println("Hello, World!")
      program.unsafeRunSync()

      lazy val loop: IO[Unit] = IO.println("loop until cancel..") >> IO.sleep(1 seconds) >> loop
      val cancel = loop.unsafeRunCancelable()


      sleep(10000)
    }

    def chapter2 = {

      val io1: IO[Unit] = IO.println("Hello") flatMap { _ =>
        IO.println("World")
      }

      io1.unsafeRunSync()
      println("---------")

      val io2: IO[Unit] = for {
        _ <- IO.println("Hello")
        _ <- IO.println("World")
      } yield ()

      io2.unsafeRunSync()
      println("---------")

      val io3: IO[Unit] = IO.println("Hello") >> IO.println("World")

      io3.unsafeRunSync()
      println("---------")



      val io4 = async[IO] {
        IO.println("Hello").await
        IO.println("World").await
      }

      io4.unsafeRunSync()

      println("---------")

    }

    def chapter3 = {
      val io =  IO.println("Ждите") >> IO(Thread.sleep(3000)) >> IO.println("Hi!")
      io.unsafeRunSync()
    }
  }

  //part1.chapter1
  //part1.chapter2
  part1.chapter3
}

object Main2 extends IOApp.Simple {
  val run = IO.println("Hello") >> IO.println("World")
}