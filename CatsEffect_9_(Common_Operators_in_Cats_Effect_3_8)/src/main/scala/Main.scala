


import cats.effect.IO
import cats.effect.unsafe.implicits.global

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


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
      // Общие операторы в Cats Effect 3 [Часть 8]

      // Введение
      // Это 8-я часть серии блогов Cats Effect 3.
      // https://yadukrishnan.live/series/cats-effect
      // В этой части давайте обсудим другие распространенные операторы/методы, доступные в Cats Effect, которые не обсуждались в предыдущих частях.

      // Блокировка
      // Как обсуждалось в предыдущих разделах, Cats Effect управляет волокнами и автоматически распределяет их по потокам.
      // Cats Effect управляет пулом потоков, который используется для всех этих операций.
      // Однако иногда нам нужно использовать блокирующие операции.
      // Поэтому вместо того, чтобы использовать тот же пул потоков, который используется волокнами CE,
      // лучше использовать отдельный пул потоков.
      // Мы можем сделать это, обернув операцию в блок IO.blocking.
      // Это гарантирует, что CE использует другой пул потоков для этой операции, и это не повлияет на основной пул потоков CE.

      val blockingPoolExec: IO[Unit] = for {
        _ <- IO(s"Hello World, welcome to CE Threadpool!").trace
        _ <- IO.blocking {
              println(s"[${Thread.currentThread.getName}] This is a blocking call, should run on different threadpool")
            }
      } yield ()

      blockingPoolExec.unsafeRunSync()

      // До версии Cats Effect 3.3.2
      // любой код, заключенный в блокирующий блок, всегда будет перемещаться в новый пул потоков.
      // Однако, начиная с версии 3.3.2, CE импровизировал блокирующий код для отдельной обработки
      // на основе пула потоков времени выполнения.
      // Поэтому он не обязательно перемещается в другой пул потоков,
      // может отличаться в зависимости от различных сценариев.
      // Подробное описание можно прочитать здесь.
      // https://github.com/typelevel/cats-effect/issues/3005#issuecomment-1134974318

      println("----------")

      // В приведенном выше примере кода оба IO будут выполняться в одном и том же пуле потоков,
      // поскольку мы используем пул потоков по умолчанию, управляемый CE.
      // Теперь, если мы изменим код так, чтобы evalOn использовал другой пул потоков, то мы увидим,
      // что код блокировки выполняется в другом пуле потоков, зарезервированном для операций блокировки:

      val customThreadPool = scala.concurrent.ExecutionContext.global
      val ioOnDiffPool: IO[Unit] = blockingPoolExec.evalOn(customThreadPool)

      ioOnDiffPool.unsafeRunSync()

      println("----------")

      // IO.interruptible

      // IO.interruptible похож на IO.blocking.
      // Но он пытается прервать операцию блокировки, если операция отменена.
      // В этом случае прерывание предпринимается только один раз.

      // Существует еще один вариант IO.interruptMany,
      // который повторяет попытку прерывания до тех пор, пока операция блокировки не будет завершена или прекращена.

      // Async

      // Async — это класс типов, который обеспечивает поддержку приостановки асинхронных операций,
      // которые происходят в другом контексте.
      // Например, мы можем переключить  результат выполнения future на IO с помощью асинхронных методов.
      // Давайте рассмотрим это на примере:

      implicit val ex: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      val future: Future[Int] = Future {
        Thread.sleep(100)
        println(s"[${Thread.currentThread.getName}] Executing the future operation")
        100
      }



      val asyncIO: IO[Int] = IO.async_[Int] { callback =>
        future.onComplete { result =>
          callback(result.toEither)
        }
      }

      val appAsyncIo: IO[Unit] = for {
        res <- asyncIO
        _ <- IO.println(s"Result = $res")
      } yield ()

      appAsyncIo.unsafeRunSync()

      // В приведенном выше коде выполнение, происходящее в потоке Future,
      // передается в IO с помощью метода async_ (обратите внимание на _ в методе).
      // Существует еще один вариант async, который является более общей версией async_

      println("----------")

      // IO.attempt

      // Метод attempt преобразует IO в Either на основе результата при выполнении.

      val simpleIO = IO("Hello World!")
      val attemptedIO: IO[Either[Throwable, String]] = simpleIO.attempt // becomes Right
      val faiureIO = IO.raiseError[String](new Exception("Some exception"))
      val attemptedFailureIOn: IO[Either[Throwable, String]] = faiureIO.attempt // becomes Left

      val attemptApp: IO[Unit] = for {
        res1 <- attemptedIO
        res2 <- attemptedFailureIOn
        _ <- IO.println(res1) >> IO.println(res2)
      } yield ()

      attemptApp.unsafeRunSync()

      println("----------")

      // IO.option

      // Подобно attempt, мы можем преобразовать IO[A] в IO[Option[A]] с помощью этого метода.
      // Если IO выполняется с ошибкой, это заменит его на None.

      val optionedIO: IO[Option[String]] = simpleIO.option
      val optionedFailureIOn: IO[Option[String]] = faiureIO.option

      val optionedApp: IO[Unit] = for {
        res1 <- optionedIO
        res2 <- optionedFailureIOn
        _ <- IO.println(res1) >> IO.println(res2)
      } yield ()

      optionedApp.unsafeRunSync()

      // Заключение
      // В этой части мы рассмотрели некоторые дополнительные операторы в Cats Effect 3.
      // Это последняя часть этой серии. Как всегда, пример кода доступен на GitHub в пакете part8.
      // https://github.com/yadavan88/cats-effect-intro
      // Надеюсь, эта серия помогла хотя бы некоторым из вас начать работу с Cats Effect 3.

    }
  }
    part1.chapter1



}

