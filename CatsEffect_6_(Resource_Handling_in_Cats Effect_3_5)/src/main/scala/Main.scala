

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.effect.kernel.Outcome._

import java.io.{BufferedReader, Closeable, File, FileReader, FileWriter}

import java.util.stream.Collectors
import scala.io.{BufferedSource, Source}
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
      // Обработка ресурсов в Cats Effect 3 [Часть 5]

      // Введение.

      // Это пятая часть серии блогов Cats Effect 3.
      // https://yadukrishnan.live/series/cats-effect
      // В этой части мы рассмотрим обработку ресурсов в Cats Effect 3.

      // Почему важно управлять ресурсами?

      // Ресурсы — это, как правило, такие вещи, как файлы, сокеты, соединения с базами данных и т. д.,
      // которые используются для чтения/записи из внешних источников.
      // Общий подход заключается в получении доступа к ресурсу, выполнении действия и закрытии ресурса.
      // Неправильное обращение с этими ресурсами, например, не закрытие после использования и т. д.,
      // может привести к очень серьезным ситуациям в любом приложении.

      // Например, если мы открываем файл для чтения и забываем закрыть его, когда закончим,
      // это приведет к утечкам памяти и возможному сбою приложения.
      // Поэтому очень важно, чтобы эти ресурсы обрабатывались осторожно.
      // Однако иногда разработчик может забыть сделать это, поскольку большинство библиотек/фреймворков осуществляют
      // проверку/принудительное исполнение.

      // Как правило, любая обработка ресурсов состоит из 3 частей:

      // - Приобрести ресурс;
      // - Используйте ресурс и выполняйте операции;
      // - Закрыть / Освободить ресурс;

      // Шаблон скобок;

      // Bracket Pattern — это способ, с помощью которого Cats Effect заставляет следовать хорошим практикам.
      // Давайте рассмотрим простой пример операции чтения файла.
      // Давайте сначала реализуем 3 основных шага обработки ресурсов:

      // acquire resource
      def getSource(fileName: String): IO[BufferedSource] = IO(Source.fromResource(fileName))

      // use resource
      def readFile(src: Source): IO[String] = IO(src.getLines().mkString).trace <* IO("Processing completed").trace

      // close/release resource
      def closeSource(src: Source): IO[Unit] = IO(src.close) <* IO("Source closed successfully").trace

      // Теперь применим шаблон скобок.
      // Мы можем применить метод скобок() к полученному IO.
      // Он принимает два параметра в качестве каррированных.
      // Первый — это использование ресурса, а вторая часть — освобождение.
      // Таким образом, мы вынуждены предоставить блок освобождения для любого ресурса, использующего шаблон скобок.
      // Это можно сравнить с блоком try-catch-finally в Java.
      // Давайте посмотрим, как мы можем объединить вышеуказанные 3 части

      val fileIO = getSource("bigfile.txt")


      val fileContent: IO[String] = fileIO.bracket(src => readFile(src))(src => closeSource(src))


      // Даже если при обработке содержимого файла возникнет какая-либо ошибка,
      // скобка обязательно вызовет closeSource() .

      // Если мы хотим обрабатывать исходный успех, неудачу и отмену по-разному, мы можем использовать
      // bracketCase вместо bracket :

      val bracketWithCase: IO[String] = fileIO.bracketCase(src => readFile(src))((src, outcome) =>
        outcome match {
          case Succeeded(s) =>
            IO("[Bracket Case] successfully processed the resource").trace >> closeSource(src) >> IO.unit
          case Errored(s) =>
            IO("[Bracket Case] Failed while processing").trace >> closeSource(src) >> IO.unit
          case Canceled() =>
            IO("[Bracket Case] Canceled the execution").trace >> closeSource(src) >> IO.unit
        }
      )

      val usebracketCase = true

      val fileApp1: IO[Unit] = for {
        content <- if (usebracketCase) bracketWithCase else fileContent
        _ <- IO.println(content)
      } yield ()


      fileApp1.unsafeRunSync()

      println("------------")

      // Это аккуратно, однако, когда задействовано больше ресурсов и они используются как вложенные ресурсы,
      // код становится немного неуклюжим.
      // Например, вот пример реализации шаблона скобок с 3 вложенными ресурсами (взято с сайта cate-effect):

      import cats.effect.MonadCancel

      def openFileToRead(str: String): IO[(BufferedReader, String)] =
        IO.println(s"Open resource to read $str") >>  IO(new BufferedReader(new FileReader(str)) -> str)

      def openFileToWrite(str:String): IO[(FileWriter, String)] =
        IO.println(s"Open resource to write $str") >>  IO(new FileWriter(new File(str)) -> str)

      def read(reader: BufferedReader): IO[String] = IO(reader.lines().collect(Collectors.joining))

      def close[A <: Closeable](resource: (A,String)): IO[Unit] = IO.println(s"Close ${resource._2}") >> IO(resource._1.close())

           val concat: IO[Unit] = MonadCancel[IO].bracket(openFileToRead("resources/file1.txt")) { file1 =>
             MonadCancel[IO].bracket(openFileToRead("resources/file2.txt")) { file2 =>
               MonadCancel[IO].bracket(openFileToWrite("resources/file3.txt")) { file3 =>
                 for {
                   str1 <- read(file1._1)
                   str2 <- read(file2._1)
                   _ <- IO(file3._1.write(str1 concat str2))
                 } yield ()
               }(file3 => close(file3))
             }(file2 => close(file2))
           }(file1 => close(file1))


          concat.unsafeRunSync()

      println("------------")

      // В этом случае Cats Effect предлагает еще один,
      // более эффективный способ управления ресурсом.

      // Ресурс.

      // Ресурс в Cats Effect — очень мощная структура данных для обработки любого типа ресурсов.
      // Она устраняет все проблемы шаблона скобок, когда задействовано несколько ресурсов.

      // Мы можем создать ресурс , используя метод make.
      // При создании ресурса мы предоставляем как реализацию acquire, так и release ресурса.
      // Например, давайте перепишем тот же файл, считанный из примера скобок:


      val fileName = "file1.txt"

      val resource: Resource[IO, Source] = Resource.make(getSource(fileName))(src => closeSource(src))


      // Теперь ресурс готов, и открытие и закрытие ресурса также уже настроены.
      // Далее мы можем использовать метод use на ресурсе для обработки содержимого:

      val fileContentUsingResource1: IO[String] = resource.use(src =>
        IO("Reading file content from 1st file ").trace >> readFile(src)
      )

      val fileApp2: IO[Unit] = for {
        content <- fileContentUsingResource1
        _ <- IO.println(content)
      } yield ()

      fileApp2.unsafeRunSync()

      println("---------")


      val anotherResource: Resource[IO, Source] = Resource.make(getSource("file2.txt"))(src => closeSource(src))

      // Мы можем объединить ресурсы, используя for-comprehension:

      val combinedResource: Resource[IO, (Source, Source)] = for {
        res1 <- resource
        res2 <- anotherResource
      } yield (res1, res2)

      // Теперь мы можем использовать оба ресурса и объединить содержимое файлов:

      val combinedContent: IO[String] = combinedResource.use { pairSources =>
        val (src1, src2) = pairSources
        for {
          content1 <- readFile(src1)
          content2 <- readFile(src2)
        } yield content1 + content2
      }

      val fileApp3: IO[Unit] = for {
        content <- combinedContent.trace
        _ <- IO.println(content)
      } yield ()

      fileApp3.unsafeRunSync()

      // Это позволит получить оба ресурса в том порядке, в котором они упомянуты.
      // Затем после использования оба ресурса будут освобождены в обратном порядке получения.

      println("---------")

      // Финализатор используя guarantee and guaranteeCase

      // Помимо скобок и Resource, Cats Effect предоставляет другой способ применения финализатора.
      // Если мы хотим выполнить блок кода по завершении IO,
      // независимо от того, был ли он успешным, неудачным или отмененным,
      // мы можем использовать guarantee.
      // Давайте рассмотрим это на простом примере:

      val successIO: IO[String] = IO("Simple IO").trace

      val successIOWithFinaliser: IO[String] =
        successIO.guarantee(IO("The IO execution finished and this finaliser is applied").trace.void)

      val app4: IO[Unit] = for {
        content <- successIOWithFinaliser
        _ <- IO.println(content)
      } yield ()

      app4.unsafeRunSync()

      // По завершении successIO будет выполнен прилагаемый гарантийный случай.

      println("---------")
      // То же самое будет применяться и в случае отказа:

      val failedIO: IO[String] =
        successIO >> IO.raiseError(new Exception("Failed during execution")).trace >> IO("IO completed")
      val failedIOWithFinaliser =
        failedIO.guarantee(IO("The IO execution finished and this finaliser is applied").trace.void)

      val app5: IO[Unit] = for {
        content <- failedIOWithFinaliser
        _ <- IO.println(content)
      } yield ()

      val runFailedApp5 = false

      if (runFailedApp5) app5.unsafeRunSync()

      // Метод guarantee() не различает случаи успеха, неудачи и отмены.
      // Для всех трех сценариев применяется один и тот же финализатор.

      println("---------")

      // Если мы хотим обрабатывать случаи по-разному, мы можем использовать вместо этого GuaranteeCase :

      def applyGuaranteeCase[A](io: IO[A]): IO[A] = {
        io.guaranteeCase {
          case Succeeded(success) =>
            success.flatMap(msg =>
              IO("IO successfully completed with value: " + msg).trace.void
            )
          case Errored(ex) =>
            IO("Error occurred while processing, " + ex.getMessage).trace.void
          case Canceled() => IO("Processing got cancelled in between").trace.void
        }
      }

      sealed trait VariantEffect
      case object Suc extends VariantEffect
      case object Err extends VariantEffect

      val variant: VariantEffect = Suc
      //val variant: VariantEffect = Err


      val effect: IO[String] = variant match {
        case Suc => successIO
        case Err => failedIO
      }

      val io: IO[String] = applyGuaranteeCase(effect)

      val app6: IO[Unit] = for {
        content <- io
        _ <- IO.println(content)
      } yield ()

      app6.unsafeRunSync()

      // Заключение

      // В этой части мы рассмотрели различные способы обработки ресурсов в Cats Effect 3.
      // Как обычно, пример кода, используемый здесь, доступен в репозитории GitHub в разделе part5.
      // https://github.com/yadavan88/cats-effect-intro
    }
  }
    part1.chapter1



}

