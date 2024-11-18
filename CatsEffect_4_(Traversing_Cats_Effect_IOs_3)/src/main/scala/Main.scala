

import cats.Traverse
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


object Main1 extends App {
  println("Изучаем Cats Effect")

  object part1 {
    def chapter1 = {
      // Обход эффектов Cats Effect IO [Часть 3]

      // 1. Введение
      // Это часть 3 серии блога Cats Effect 3.
      // В предыдущих двух частях мы рассмотрели различные способы создания IO,
      // а также некоторые распространенные методы, применяемые к IO. Вы можете обратиться к полной серии здесь.
      // https://yadukrishnan.live/series/cats-effect

      // В этой части давайте рассмотрим различные способы объединения и обхода нескольких IO.

      // 2. map, flatMap, for-comprehension.

      // В предыдущих частях мы уже изучили использование map, flatMap и for-comprehension.
      // Они помогают сцеплять несколько типов IO и работать с каждым из них.
      // Мы также рассмотрели другие комбинаторы, такие как >>, *> и &> которые также выполняют сцепку,
      // хотя и с некоторыми небольшими отличиями.
      // За исключением &> другие методы, выполняют IO последовательно в том же потоке.

      // Мы можем проверить это на небольшом примере, добавив имя потока к операторам печати.
      // Сначала давайте создадим метод расширения на IO, чтобы добавить оператор печати.
      // Это поможет напечатать имя потока при выполнении IO.

      // Scala 3
      /*
           extension [A](io: IO[A])
            def trace: IO[A] = for {
              res <- io
              _ = println(s"[${Thread.currentThread.getName}] " + res)
            } yield res

       */

      // Scala 2
      implicit class  IOTrace[A](io: IO[A]) {
        def trace: IO[A] = for {
          res <- io
          _ = println(s"[${Thread.currentThread.getName}] " + res)
        } yield  res
      }

      val io1 = IO.sleep(10 millis)  >> IO("Hello ")
      val io2 = IO.sleep(10 millis)  >> IO("World ")

      val forCombined: IO[Unit] = for {
        res1 <- io1.trace
        res2 <- io2.trace
      } yield ()

      forCombined.unsafeRunSync()


      // Когда мы выполним forCombined IO, мы увидим, что имя потока будет таким же.
      // Это будет применяться для flatMap, >> и *> также.

      println("-------------------------")

      // Давайте попробуем использовать &>

      val parCombined: IO[String] = io1.trace &> io2.trace

      val respar: String = parCombined.unsafeRunSync()

      //Когда parCombined запущен, мы можем заметить, что имя потока отличается для каждого IO.
      // Это означает, что каждый из IO запускается параллельно, а затем объединяется.
      println("-------------------------")

      // 3. Использование методов mapN и tupled.

      // Вышеуказанные методы вернут только один из результатов участвующих IO, если явно не обработано.
      // Мы можем получить результат всех участвующих IO, используя mapN расширенный метод из cats.
      // Сначала нам нужно добавить оператор импорта, чтобы перенести метод в область действия:

      import cats.syntax.apply._

      // Теперь мы можем применить метод mapN:

      val catMapN: IO[(String, String)] = (io1,io2).mapN((i,j) => (i,j))

      println{
        catMapN.unsafeRunSync()
      }

      // Если нам просто нужно объединить 2 IO в кортеж без выполнения каких-либо преобразований,
      // мы также можем использовать tupled метод:

      val catTupled: IO[(String, String)] = (io1,io2).tupled

      println{
        catTupled.unsafeRunSync()
      }

      // Обратите внимание, что методы mapN и tupled будут выполнять эффекты только последовательно.

      println("-------------------------")

      // Параллельное выполнение с использованием parMapN и parTupled
      // Если мы хотим выполнять IO параллельно, мы можем использовать parMapN.
      // Это похоже на &>, но помогает применять больше преобразований к результатам.
      // Нам нужно добавить следующий оператор импорта для использования parMapN

      import cats.syntax.parallel._

      // Затем мы можем использовать, как показано ниже.
      // Обратите внимание, что метод трассировки применяется для проверки того, что используются разные потоки:

      val parMapIO: IO[String] = (io1.trace, io2.trace).parMapN(_ + _).trace

      println {
        parMapIO.unsafeRunSync()
      }

      // Если мы хотим просто вычислить оба эффекта параллельно и получить результат в виде кортежа,
      // мы также можем использовать parTupled.

      val parTupledIO: IO[(String, String)] = (io1.trace, io2.trace).parTupled.trace

      println {
        parTupledIO.unsafeRunSync()
      }

      println("-------------------------")

      // Преобразование IO изнутри наружу с использованием обхода и последовательности

      // Иногда у нас может быть коллекция IO. Становится сложно использовать такую коллекцию на IO вместе.
      // Вместо этого, будет проще, если мы преобразуем ее в IO коллекций.

      // Некоторые из вас, возможно, знакомы с методом Future.sequence для преобразования List[Future] в Future[List].
      // Мы можем сделать то же самое и в IO. Давайте посмотрим, как можно использовать sequence в IO.

      // Во-первых, нам нужно получить экземпляр typeclass требуемой коллекции из библиотеки cats.
      // Обратите внимание, что этот typeclass определен в cats, а не в cats-effect.

      val listTraverse = Traverse[List]

      // Теперь мы можем применить последовательность к IOs следующим образом:

      val ioList1: List[IO[String]] = List(io1, io2)
      val insideOutIOs1: IO[List[String]] = listTraverse.sequence(ioList1)

      println {
        insideOutIOs1.unsafeRunSync()
      }

      // Метод последовательности просто делает коллекцию IO наизнанку.
      // Однако есть еще один очень мощный метод, который также может применить некоторое преобразование,
      // одновременно вынося результат наизнанку. Он называется traverse.

      // Метод traverse также применяется к тому же экземпляру класса типов Traverse.
      // Он принимает 2 параметра в качестве каррированных.
      // Первый — это коллекция IO.
      // Вторая часть — это функция, которая обрабатывает каждый IO в коллекции.
      // Давайте рассмотрим это на примере для большей ясности:

      val ioList2: List[IO[String]] = List(io1, io2)
      val insideOutIOs2: IO[List[String]] = listTraverse.sequence(ioList2)

      val traversedList: IO[List[String]] = listTraverse.traverse(ioList2)(io => io.map(_ + "!"))

      println {
        traversedList.unsafeRunSync()
      }

      // Вторая функция, которая добавляет !символ к строке, передается как вторая часть traverse.

      // Мы можем реализовать метод последовательности, который мы видели ранее, используя обход и функцию тождественности:

      val seqAsTraverse: IO[List[String]] = listTraverse.traverse(ioList2)(identity)

      println {
        seqAsTraverse.unsafeRunSync()
      }

      println("-------------------------")

      // Методы параллельного обхода и последовательности
      // Подобно mapN, есть параллельная версия для traverse и sequence.
      // Они называются parTraverse и parSequence.
      // Эти методы доступны как методы расширения из cats и должны быть импортированы для использования:

      // import cats.syntax.parallel._

      val parTraverseIOs: IO[List[String]] = ioList2.parTraverse(identity)
      val parSeq: IO[List[String]] = ioList2.parSequence

      println(parTraverseIOs.unsafeRunSync())
      println(parSeq.unsafeRunSync())

      // Заключение

      // В этой части мы в основном рассматривали traverse, sequence, parTraverse и parSequence для обработки коллекции IO.
      // Примеры кода, упомянутые в этой статье, доступны на GitHub в пакете part3.
      // https://github.com/yadavan88/cats-effect-intro

    }

  }

  part1.chapter1

}

