


import cats.effect.IO
import cats.effect.unsafe.implicits.global


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
      // Обработка ошибок в Cats Effect [Часть 9]

      // 1. Введение
      // Это еще одна часть серии Cats Effect 3.
      // https://yadukrishnan.live/series/cats-effect
      // В этой короткой части давайте рассмотрим некоторые способы обработки ошибок в Cats Effect 3.

      // 2. Вызов ошибки
      // Во-первых, давайте посмотрим, как мы можем вызвать ошибку в CE.
      // Мы можем создать неудачный экземпляр IO, используя метод raiseError:

      val failedIO: IO[Int] = IO.raiseError(new RuntimeException("Boom!"))

      // 3. Обработка ошибок
      // Теперь давайте рассмотрим различные способы обработки значений ошибок в CE.

      // Мы можем обрабатывать ошибки ввода-вывода с помощью методов handleError и handleErrorWith:
      val handledError: IO[Int] = failedIO.handleError(ex => 500)
      val handledErrorWith: IO[Int] = failedIO.handleErrorWith(ex => IO(-2))

      // В случае сбоя выполняется блок handleError и это значение возвращается вместо входного значения.
      // Нам нужно вернуть IO из handleErrorWith блока, тогда как из блока возвращается простое значение handleError.

      // Мы также можем использовать методы restore и restoreWith для преобразования эффекта неудачного IO:

      val recoveredFailedIO: IO[Int] = failedIO.recover {
        case ex => 0
      }

      // Метод recover принимает частичную функцию, и мы можем применить сопоставление с образцом к исключениям,
      // чтобы вернуть желаемое значение.
      // В предыдущем примере мы можем вернуть значение 0 в случае сбоя.
      // Аналогично, мы можем использовать recoverWith для возврата эффективного значения внутри частичной функции
      // вместо простого значения:

      val recoveredWithFailedIO: IO[Int] = failedIO.recoverWith {
        case ex => IO(0)
      }

      // В этом случае нам необходимо вернуть IO в рамках сопоставления с образцом.

      // Это похоже на методы restore и restoreWith, доступные в Future.

      // Главное отличие между handleError и recover методом заключается в том,
      // что recover метод принимает частичную функцию,
      // что позволяет нам легко обрабатывать различные типы ошибок разными способами.
      // Давайте рассмотрим пример:

      val errorIO = IO(100/0)
      val recoveredFailedIO_2: IO[Int] = errorIO.recover {
        case ex: ArithmeticException => 0
      }

      // В приведенном выше случае errorIO значение будет преобразовано в IO(0) в случае ArithmeticException.
      // Для любых других ошибок будет возвращено то же значение сбоя, поскольку у нас нет соответствующего case оператора для них.



      val app1: IO[Unit] = for {
        res1 <- handledError
        res2 <- handledErrorWith
        res3 <- recoveredFailedIO
        res4 <- recoveredWithFailedIO
        res5 <- recoveredFailedIO_2
        _ <- IO.println((res1, res2, res3, res4, res5))
      } yield ()

      app1.unsafeRunSync()

      println("---------")

      // Если мы хотим просто выполнить побочный эффект при сбое,
      // например, зарегистрировать исключение, мы можем просто использовать метод onError :

      val loggedFailedError: IO[Int] = failedIO.onError(ex => IO.println("It failed with message: "+ex.getMessage))

      val failedApp1: IO[Unit] = for {
        _ <- loggedFailedError
      } yield ()

      failedApp1.handleError(ex => 0).unsafeRunSync()

      // Используя onError(), мы регистрируем сообщение об ошибке в консоли и возвращаем тот же эффект IO обратно.
      // В Scala Future нам нужен failedFuture.failed.foreach метод do для выполнения действия над неудачным future.

      println("---------")

      // Мы можем использовать метод attempt для подъема значения IO в Either.
      // Если IO не удалось, он поднимает значение как Right, в противном случае в Left :

      val attemptedIO: IO[Either[Throwable, Int]] = loggedFailedError.attempt

      val failedApp2: IO[Unit] = for {
        res <- attemptedIO
        _ <- IO.println(res)
      } yield ()

      failedApp2.unsafeRunSync()

      println("---------")

      // Существует метод rethrow(), который является обратным методу attempt.
      // Он преобразует IO[Either[Throwable, A]] в IO[A].
      // Если любой из них является Right, значение будет поднято в IO,
      // в противном случае это будет неудачный IO:

      val eitherValue: IO[Either[Throwable, String]] = IO.pure(Right("Hello World"))
      val rethrownValue: IO[String] = eitherValue.rethrow

      // Другой способ работы с неудавшимся вводом-выводом — использовать orElse():


      val orElseResult1: IO[Int] = IO(100).orElse(IO(-1)) //returns IO(100)
      val orElseResult2: IO[Int] = failedIO.orElse(IO(-1)) //returns IO(-1)

      val app3 = for {
        r1 <- eitherValue
        r2 <- rethrownValue
        r3 <- orElseResult1
        r4 <- orElseResult2
        _ <- IO.println((r1,r2,r3,r4))
      } yield ()

      app3.unsafeRunSync()

      // 4. Заключение
      // В этой короткой части мы обсудили различные опции, доступные в Cats Effect для обработки ошибок.
      // Пример кода доступен здесь на GitHub.
      // https://github.com/yadavan88/cats-effect-intro/tree/main/src/main/scala/com/yadavan88/ce/part9

    }
  }
    part1.chapter1



}

