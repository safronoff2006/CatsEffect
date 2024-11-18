

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


object Main1 extends App {
  println("Изучаем Cats Effect")

  object part1 {
    def chapter1 = {
      // Введение
      // В части 1 этой серии
      // https://yadukrishnan.live/cats-effect-3-for-beginners-part-1
      // мы рассмотрели Cats Effect 3 и то, как запустить простое приложение, написанное с его помощью.
      // В этой части давайте рассмотрим некоторые общие API и методы, используемые с типом данных IO.

      // map, flatMap and for-comprehension

      // Поскольку IO — это монада, мы можем использовать общие операции, такие как map и flatMap, для связывания IO.
      // В качестве альтернативы мы также можем использовать for-comprehension.

      val io1 = IO("Scala")
      val io2 = IO("Cats")
      val mapPgm: IO[String] = io1.map(_.toUpperCase)
      val flatMapPgm: IO[String] = io1.flatMap(a => io2.map(a + _))
      val forComp: IO[String] = for {
        a <- io1
        b <- io2
      } yield a + b

      // Мы также можем использовать flatten, чтобы избежать вложенных операций ввода-вывода:
      val aIO: IO[String] = IO("Hello")
      val anotherIO: IO[String] = IO(aIO).flatten

      //Cats Effect также предоставляет еще один полезный метод для решения этой задачи с использованием defer :
      val deferredIO: IO[String] = IO.defer(IO("String"))

      // Обратите внимание, что IO.delay(anotherIO) создаст вложенный IO,
      // тогда как IO.defer(IO) вернет свернутый IO.

      // Другие простые методы
      // Чтобы отменить результат вычисления эффекта, мы можем использовать void.

      val strIO: IO[String] = IO("Cats Effect")
      val voidIO: IO[Unit] = strIO.void

      // Чтобы заменить значение в IO другим значением, мы можем использовать as вместо map :
      val asIntIO: IO[Int] = strIO as 100

      // Чтобы создать эффект, который выводит содержимое на консоль,
      // мы можем использовать вспомогательный метод IO.println() :

      val printIO = IO.println("Hello World")

      // Обработка исключений
      // Поскольку IO описывает эффект, есть вероятность, что оценка может потерпеть неудачу во время выполнения.
      // Тип данных IO также может фиксировать такие неудачи.
      // Мы можем использовать raiseError для неудачи IO.

      val aFailedIO: IO[String] = IO.raiseError[String](new Exception("Failed IO"))


      // Если мы хотим создать неудачный IO на основе некоторого условия,
      // мы можем использовать IO.raiseWhen().
      // Он вызывает указанное исключение, если условие соответствует, иначе возвращает IO[Unit].
      // Это особенно полезно для обработки некоторых нежелательных ситуаций.

      val num = 0

      val raisedIO: IO[Unit] = IO.raiseWhen(num == 0)(new RuntimeException("Number can not be 0"))

      // Аналогично, IO.raiseUnless() вызывает исключение, если условие не выполняется.

      // Обработка ошибок
      //  Мы можем обрабатывать ошибки из IO с помощью метода handleError() .
      //  Это похоже на восстановление в Future.
      //  (Это похоже на handleError в MonadError).
      val aFailedIntIO: IO[Int] = IO.raiseError[Int](new Exception("Failed IO"))

      val handledIO: IO[Int] = aFailedIntIO.handleError(ex => 0)

      println {
        handledIO.unsafeRunSync()
      }

      // Мы также можем использовать handleErrorWith таким же образом, как мы используем restoreWith в Future.
      // Обработчик внутри handleErrorWith должен возвращать другой IO.

      val handledWithIO: IO[Int] = aFailedIntIO.handleErrorWith(_ => IO.pure(-1))

      println {
        handledWithIO.unsafeRunSync()
      }

      // Мы также можем использовать метод redeem для обработки как успешных,
      // так и неудачных случаев одновременно.
      // Это похоже на метод transform в Future.
      // Однако redeem сначала выполняет обработку исключений, а затем обработку успехов.

      val  intIO = IO(40)
      val redeemedIO: IO[String] = intIO.redeem(_ => "failed", _ => "success")

      println {
        redeemedIO.unsafeRunSync()
      }

      // Поднятие общих типов в IO
      // IO предоставляет несколько вспомогательных методов для преобразования большинства распространенных типов Scala в IO.

      // IO.fromOption поднимает Option[A] до IO[A]
      // IO.fromTry поднимает Try[A] до IO[A]
      // IO.fromEither поднимает Either[Throwable, A] до IO[A]
      // Мы также можем поднять Future до IO, используя метод fromFuture.
      // Однако, чтобы приостановить выполнение Future, нам сначала нужно обернуть Future в IO.

      implicit val ce =  scala.concurrent.ExecutionContext.Implicits.global
      lazy val aFuture: Future[Int] = Future(100)
      val ioFromFuture: IO[Int] = IO.fromFuture(IO(aFuture))

      println {
        ioFromFuture.unsafeRunSync()
      }

      // Цепочка операций ввода-вывода
      // Мы можем использовать map, flatMap и for-comprehension для цепочки IO.
      // Однако есть и другие комбинаторы, которые доступны для цепочки различных IO.

      // Давайте определим два IO, чтобы объяснить цепочку:

      val firstIO: IO[Int] = IO(100)
      val secondIO: IO[String] = IO("Millions")

      // Теперь применим цепочку с использованием *> комбинатора:

      val firstSecond: IO[String] = firstIO *> secondIO

      // Он выполнит firstIO , затем secondIO , отбросит результат firstIO и вернет результат secondIO .

      // Теперь давайте используем <* комбинатор.
      // Он запускает IO в том же порядке, но сохраняет результат первого и игнорирует результат второго:

      val secondFirst: IO[Int] = firstIO <* secondIO

      // Есть еще один комбинатор >>, который почти похож на *>.
      // Единственное отличие в том, что >> он лениво вычисляется и, следовательно, безопасен для стека.
      // Рассмотрим, >> задействована ли рекурсия:

      val anotherCombinator: IO[String] = firstIO >> secondIO

      // Существует еще одна версия &> и <&, которая похожа на >> и <<,
      // но выполняет эффекты параллельно, а не последовательно.

      // IO Sleep

      // Cats Effect обеспечивает очень чистый способ асинхронного перевода IO в спящий режим на указанное время.
      // Это называется семантической блокировкой.

      val sleepingIO = IO.sleep(100 millis)

      // Обратите внимание, что в отличие от Thread.sleep,
      // IO.sleep НЕ блокирует поток.
      // Cats Effect будет внутренне управлять потоками и обрабатывать сон асинхронно.

      println {
        (
            IO.println("Раз") >>
            IO.sleep(1000 millis) >>
            IO.println("Два") >>
            IO.sleep(1000 millis) >>
            IO.println("Три")

          ).unsafeRunSync()
      }

      // IO Never
      // Мы можем создать никогда не завершающийся эффект, используя метод never.

      val neverEndingIO = IO.println("Start") >> IO.never >> IO.println("Done")
      neverEndingIO.unsafeRunSync()

      // В приведенном выше коде последний IO, который выводит «Done», никогда не будет выполнен,
      // поскольку непосредственно перед ним находится IO.never.

      // Заключение
      // В этой части мы рассмотрели некоторые из распространенных и наиболее полезных комбинаторов
      // и методов в структуре данных IO. Пример кода, используемый здесь, доступен на GitHub в пакете part2.
      // https://github.com/yadavan88/cats-effect-intro



    }

  }

  part1.chapter1

}

