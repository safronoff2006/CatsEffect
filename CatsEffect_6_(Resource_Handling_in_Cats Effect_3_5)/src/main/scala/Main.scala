

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import cats.effect.kernel.Outcome._

import java.io.{BufferedReader, Closeable, File, FileReader, FileWriter, InputStreamReader}
import java.nio.CharBuffer
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

      // Это аккуратно, однако, когда задействовано больше ресурсов и они используются как вложенные ресурсы,
      // код становится немного неуклюжим.
      // Например, вот пример реализации шаблона скобок с 3 вложенными ресурсами (взято с сайта cate-effect):

      import cats.effect.MonadCancel

      def openFileToRead(str: String): IO[(BufferedReader, String)] =IO(new BufferedReader(new FileReader(str)) -> str)
      def openFileToWrite(str:String): IO[(FileWriter, String)] =IO(new FileWriter(new File(str)) -> str)
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

      // В этом случае Cats Effect предлагает еще один,
      // более эффективный способ управления ресурсом.



    }
  }
    part1.chapter1



}

