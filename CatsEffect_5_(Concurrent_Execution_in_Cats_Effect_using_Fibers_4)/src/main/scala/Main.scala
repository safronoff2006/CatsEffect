


import cats.effect.kernel.Outcome._
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Outcome}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random


object Main1 extends App {
  println("Изучаем Cats Effect")

  //////////////// служебное - трассировка IO-эффекта ///////
      implicit class  IOTrace[A](io: IO[A]) {
        def trace: IO[A] = for {
          res <- io
          _ = println(s"[${Thread.currentThread.getName}] " + res)
        } yield  res
      }
  ////////////////////////////////////////////////////////////

  object part1 {
   def chapter1 = {


    // https://yadukrishnan.live/concurrent-execution-in-cats-effect-using-fibers

    // Конкурентное выполнение в Cats Effect с использованием волокон [Часть 4]
    // Введение.

     // Это четвертая часть серии блогов Cats Effect 3.
     // https://yadukrishnan.live/series/cats-effect
     // В предыдущей части мы рассмотрели различные способы обхода и цепочек IO.
     // https://yadukrishnan.live/traversing-cats-effect-ios-part-3
     // Мы также рассмотрели параллельные API Cats Effect IO.
     // В этой части мы рассмотрим одну из самых важных функций Cats Effect 3, называемую Fibers.

     // Concurrency vs Parallelism.

     // Прежде чем изучать волокна, давайте сначала разберемся в разнице между параллельным и одновременным выполнением.

     // Параллелизм — это способ, при котором несколько вычислений выполняются в одно и то же время.
     // Обычно это использует несколько ядер процессора для выполнения каждого вычисления.
     // Методы parSequence, parTraverse и т. д. используют этот подход.

     // Параллелизм не обязательно подразумевает, что вычисления выполняются одновременно.
     // Вместо этого они чередуются.
     // Это означает, что несколько задач выполняются одним и тем же потоком,
     // переключаясь между ними, когда это возможно.
     // Другими словами, рассматривайте это как жонглирование между различными задачами без потери свободного времени.

     // Очень подробное объяснение можно прочитать на странице документации Cats Effect.
     // https://typelevel.org/cats-effect/docs/2.x/concurrency/basics

     // resources/concurrency-vs-parallelism.png

     // Волокна.

     // Волокна похожи на очень легкие потоки в Cats Effect 3.
     // Они являются строительными блоками модели параллелизма Cats Effect.
     // Мы можем создавать столько волокон, сколько нужно, не беспокоясь о потоке и пуле потоков.
     // Эти волокна можно создавать, объединять или отменять.
     // Среда выполнения Cats Effect заботится о планировании этих волокон в реальных потоках.
     // Поэтому разработчику вообще не нужно беспокоиться о базовых потоках или о том, как с ними обращаться.

     // Волокна реализуют семантическую блокировку, что означает, что базовые потоки не блокируются средой выполнения CE.
     // Кроме того, волокно не привязано к определенному потоку.
     // Следовательно, волокно может выполняться в нескольких потоках до завершения задачи в зависимости от доступности потоков.
     // Эти волокна можно в общих чертах сравнить с Akka Actors,
     // хотя модель актеров представляет собой совершенно другую парадигму.

     // Использование.
     // Теперь давайте посмотрим, как мы можем создавать и использовать волокна для обработки параллельного выполнения.
     // Мы можем вызвать .start метод на IO, чтобы создать волокно.
     // Мы можем использовать .join метод на волокне, чтобы ждать результата.
     // Давайте рассмотрим это на примере:



     val io: IO[Int] = IO("Long computation").trace >> IO.sleep(1.second) >> IO(
       Random.nextInt(1000)
     ) <* IO("Computation done!").trace

     val fibersIO: IO[Outcome[IO, Throwable, Int]] = for {
       fib <- io.start
       fiberOutcome <- fib.join
     } yield fiberOutcome

     // В приведенном выше коде io.start создает волокно и выполняет IO в другом потоке.
     // trace метод расширения, который мы создали для IO, выведет имя потока, в котором выполняется IO.
     // Таким образом, мы можем проверить параллельное выполнение волокон.
     // Мы можем вызвать join метод для дескриптора волокна fib,  чтобы получить результат выполнения.
     // Он возвращает тип Outcome, который содержит различные возможности выполнения волокна.
     // Три возможности — Succeeded, Errored и Canceled.
     // Мы можем выполнить сопоставление с образцом для результата, чтобы обработать результаты:


     /*
              fiberOutcome match {
               case Succeeded(fa) => IO.println("Fiber succeeded with result: ") >> fa.trace
               case Errored(e)    => IO.println("Fiber execution failed with exception: "+e)
               case Canceled()    => IO.println("Fiber operation got cancelled")
             }
      */

     // Отмена эффекта.

     // Одной из самых мощных функций волокна является возможность отмены его выполнения.
     // Мы можем вызвать cancel метод на дескрипторе волокна, чтобы отменить текущее волокно.
     // Давайте рассмотрим это на примере кода:

     // Эффект без прерывания
     val fiberCancellation1: IO[Unit] = for {
       fiber <- io.start
       result <- fiber.join
       _ <- result match {
         case Succeeded(fa) => IO.println("Fiber succeeded with result: ") >> fa.trace
         case Errored(e) => IO.println("Fiber execution failed with exception: " + e)
         case Canceled() => IO.println("Fiber operation got cancelled")
       }
     } yield ()
     fiberCancellation1.unsafeRunSync()

     println("---------")

     // эффект с прерыванием
     val fiberCancellation2: IO[Unit] = for {
       fiber <- io.start
       _ <- IO.sleep(400.millis) >> fiber.cancel
       result <- fiber.join
       _ <- result match {
         case Succeeded(fa) => IO.println("Fiber succeeded with result: ") >> fa.trace
         case Errored(e) => IO.println("Fiber execution failed with exception: " + e)
         case Canceled() => IO.println("Fiber operation got cancelled")
       }
     } yield ()
     fiberCancellation2.unsafeRunSync()

     println("---------")

     // io — это длительная операция, которая занимает чуть больше 1 секунды.
     // Волокно отменяется через 400 миллисекунд путем вызова метода отмены на дескрипторе волокна.
     // Приведенный выше код выведет вывод как Fiber operation got cancellation.

     // Мы также можем прикрепить хук отмены к IO с помощью onCancel().
     // Давайте изменим приведенный выше пример, чтобы использовать вместо этого onCancel :


     val ioWithHook: IO[Int] = io.onCancel(IO("Finaliser for IO Fiber cancellation executed").trace.void)

     val cancel = true
     // val cancel = false

     val fiberCancellationV2: IO[Outcome[IO, Throwable, Int]] = for {
       fiber <- ioWithHook.start
       _ <- if (cancel)  IO.sleep(100.millis) >> IO.println("It's cancel") >> fiber.cancel
            else IO.println("NO cancel")
       result <- fiber.join
     } yield result

     val fiberCancellation3: IO[Unit] = for {
       result <- fiberCancellationV2
       _ <- result match {
         case Succeeded(fa) => IO.println("Fiber succeeded with result: ") >> fa.trace
         case Errored(e) => IO.println("Fiber execution failed with exception: " + e)
         case Canceled() => IO.println("Fiber operation got cancelled")
       }
     } yield ()

     fiberCancellation3.unsafeRunSync()

     // Вместо вызова запуска волокна на io, мы создаем новый IO ioWithHook с примененным хуком onCancel.
     // Для этого IO инициируется волокно. Когда волокно отменяется, оно вызывает хук/финалист onCancel.
     // Обратите внимание, что io.onCancel() создает новый тип IO с прикрепленным хуком и на исходном io,
     // это не окажет никакого влияния.



   }
  }

  part1.chapter1

  // Заключение.

  // В этой части мы рассмотрели Cats Effect 3 Fibers и связанные с ними API.
  // Волокна — очень важная концепция, и многие из расширенных параллельных функций используют волокна
  // под капотом для реализации.
  // Пример кода, используемый здесь, доступен на GitHub в пакете part4.
  // https://github.com/yadavan88/cats-effect-intro

}

