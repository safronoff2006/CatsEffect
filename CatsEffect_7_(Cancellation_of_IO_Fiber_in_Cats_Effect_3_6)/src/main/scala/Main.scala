

import cats.effect.{FiberIO, IO, OutcomeIO}
import cats.effect.unsafe.implicits.global

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
      // Отмена IO/Fiber в Cats Effect 3 [Часть 6]

      // Введение
      // Это шестая часть серии Cats Effect.
      // https://yadukrishnan.live/series/cats-effect
      // В части 4 этой серии мы рассмотрели параллельное программирование в Cats Effect 3 с использованием Fibers.
      // https://yadukrishnan.live/concurrent-execution-in-cats-effect-using-fibers
      // В этой части мы рассмотрим отмену уже запущенных IO/Fibers.

      // Отмена

      // Одной из самых ценных особенностей волокон является возможность их отмены, если они больше не нужны.
      // Таким образом, мы можем сэкономить ресурсы, а также избежать использования ЦП нежелательными волокнами.
      // Мы можем использовать метод отмены на дескрипторе волокна, чтобы отменить работающее волокно:

      val longRunningIO: IO[String] =
        (IO("Start processing").trace >> IO.sleep(500.millis) >> IO(
          "Task completed"
        ).trace)
          .onCancel(IO("This IO got cancelled").trace.void)

      val fiberOps: IO[Unit] = for {
        fib <- longRunningIO.start
        _ <- IO.sleep(200.millis) >> IO("cancelling task!").trace
        _ <- fib.cancel
        res <- fib.join
      } yield ()

      fiberOps.unsafeRunSync()

      // Таким образом, мы можем отменить выполнение волокна вручную.
      // Однако есть и другие способы, с помощью которых можно выполнить отмену волокон.
      // Некоторые из этих методов используют простой метод отмены под капотом,
      // но он еще больше упрощает задачу для разработчиков, чтобы не беспокоиться об отмене вручную.

      println("-------------------")

      // Гонки волокон

      // Вместо того, чтобы вручную обрабатывать жизненный цикл волокна,
      // мы можем использовать race two IOs и взять результат первого завершенного.
      // Таким образом, среда выполнения эффекта cats автоматически создаст и обработает жизненный цикл волокон.
      // Разработчикам не нужно беспокоиться об отмене медленного и присоединении быстрого.

      // Давайте рассмотрим это на примере сценария.
      // Предположим, что мы делаем запрос из двух разных служб и используем результат первой завершенной службы,
      // чтобы продолжить. Мы можем использовать простое волокно для обеих операций и отменить другое волокно,
      // когда одно из волокон завершит задачу.

      // IO.race()

      // Та же логика может быть реализована с использованием race.
      // Среда выполнения эффекта cats будет управлять волокнами, отменять медленное волокно и возвращать результат из быстрого.
      // Давайте рассмотрим простой пример:


      val io1 = IO("Task 1 starting..").trace >> IO.sleep(Random.nextInt(1000).millis).trace >>
        IO("Task 1 completed").trace
      val io2 = IO("Task 2 starting..").trace >> IO.sleep(Random.nextInt(1000).millis).trace >>
        IO("Task 2 completed").trace

      val raceIOs: IO[Either[String, String]] = IO.race(io1, io2)
      val raceResult: IO[Unit] = raceIOs.map {
        case Right(res) => println(s"io2 finished first: `${res}` ")
        case Left(res) => println(s"io1 finished first: `${res}` ")
      }
      raceResult.unsafeRunSync()

      // В приведенном выше примере кода IOs io1 и io2 завершают задачу на основе предоставленного значения sleep.
      // Когда IO.race() вызывается с io1 и io2, первый завершенный результат возвращается как результат Either.
      // Если io1 завершается первым, результат Either будет завершен со значением Left,
      // а в противном случае — со значением Right.
      // Здесь завершение не обязательно означает успешное выполнение.
      // Если IO быстро терпит неудачу, то другой немедленно отменяется.

      // IO.racePair

      // RacePair — более общая версия race() .
      // Вместо отмены медленного волокна он возвращает дескриптор этого волокна вместе с результатом первого.
      // Затем разработчик может принять решение о немедленной отмене,
      // выполнить какие-то другие операции перед отменой или не отменять вообще.

      // Вместо того, чтобы возвращать простое Either с результатом,
      // racePair возвращает Either из кортежа с Outcome завершенного волокна и дескриптора другого волокна.
      // Это позволяет более точно контролировать волокна во время их гонки.

      // Давайте рассмотрим это на другом простом примере:

      val racePairResult: IO[Either[(OutcomeIO[String], FiberIO[String]), (FiberIO[String], OutcomeIO[String])]] =
              IO.racePair(io1, io2)


      println("--------------------")

      // Тайм-аут IO

      // Cats Effect реализует метод тайм-аута на IO, используя ту же отмену волокна.
      // Мы можем отменить выполнение IO, если оно занимает больше желаемой продолжительности, используя метод timeout().
      // Например, давайте посмотрим, как можно убедиться, что IO занимает не более 500 миллисекунд для выполнения,
      // и вызвать исключение, если это занимает больше времени.

      val maybeSlowIO = (IO("Task is starting..").trace >> IO
        .sleep(Random.nextInt(1000).millis)
        .trace >> IO("Task is completed").trace)
        .onCancel(IO("Cancelled this IO since it is slow").trace.void)

      val ioExec: IO[String] = maybeSlowIO.timeout(500.millis)

      val appIoExec1 = for {
        res <- ioExec
        _ <- IO.println(res)
      } yield ()

      //appIoExec1.unsafeRunSync()

      // Если IO maybeSlowIO занимает более 500 миллисекунд, он будет отменен и будет вызвано исключение TimeoutException.
      // Этот метод очень полезен при обработке операций со строгой длительностью.

      // Есть еще одна вариация таймаута как timeoutTo,
      // которая позволяет выполнить резервный IO в случае таймаута.
      // Мы можем переписать код примера таймаута выше как:

      val timeoutWithFallback: IO[String] = maybeSlowIO.timeoutTo(500.millis, IO("Fallback IO executed").trace)

      val appIoExec2 = for {
        res <- timeoutWithFallback
        _ <- IO.println(res)
      } yield ()

      appIoExec2.unsafeRunSync()

      println("--------------------")

      // Неотменяемый

      // До сих пор мы рассматривали различные способы отмены IO или Fiber.
      // Иногда нам нужно убедиться, что некоторая часть выполнения не будет отменена.
      // Например, после операции базы данных, если мы обновляем кэш, нам нужно убедиться, что операция обновления завершена.
      // В противном случае это может привести к грязному кэшу, если операция была отменена между ними.
      // Мы можем сделать блок кода свободным от отмены, используя неотменяемый метод.

      // Используя uncancelable, мы можем пометить код как неотменяемый.
      // Даже если будет подан запрос на отмену, отмена любого кода в блоке uncancelable будет отклонена.
      // Давайте посмотрим, как это можно сделать:

      val step1 = IO("Step 1").trace
      val importantTask = IO.uncancelable(unmask =>
        IO("uncancelable start").trace >> IO.sleep(1.second) >> IO("task completed") >> IO("uncancelable end").trace
      )
      val step3 = IO("Final step ").trace

      val tryCancel: IO[Unit] = for {
        _ <- step1
        fib <- importantTask.start
        res <- IO.sleep(400.millis) >> IO("trying to cancel importantTask").trace >> fib.cancel
        _ <- fib.join
      } yield ()

      tryCancel.unsafeRunSync()

      // В приведенном выше коде importantTask занимает 1 секунду для завершения.
      // Однако мы пытаемся отменить волокно через 400 миллисекунд (т. е. до завершения выполнения).
      // Но вызов отмены не влияет на это волокно, поскольку importantTask обернут в неотменяемый блок.
      // Обратите внимание, что мы можем сделать часть блока отменяемой, обернув его с помощью обратного вызова unmask в блоке.

      // Если мы хотим сделать весь IO неотменяемым, мы можем использовать uncancelable для IO следующим образом:

      val fullUncancelable = importantTask.uncancelable

      // Мы можем создать несколько регионов неотменяемых IO,
      // используя обратный вызов unmask.
      // Только блок, заключенный в обратный вызов mask, станет отменяемым, вне его IO становится неотменяемым.
      // Например:


      val unmaskBlocks = IO.uncancelable(unmask => unmask(IO("Step1")) >> IO("Step2") >> unmask(IO("Step3")))

      // В приведенном выше коде шаги step1 и step3 можно отменить, так как они заключены в unmask,
      // тогда как step2 отменить НЕЛЬЗЯ.
      // ** Блок unmask помогает отменить часть цепочки, которую иначе нельзя отменить. **

      // Заключение
      // В этой части мы рассмотрели волокна и различные способы отмены.
      // Пример кода, используемый здесь, доступен на GitHub в пакете part6.
      // https://github.com/yadavan88/cats-effect-intro


    }
  }
    part1.chapter1



}

