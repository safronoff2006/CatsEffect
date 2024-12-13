

import cats.{Functor, Monad}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import cats.implicits._

import scala.util.Try


object Main1 extends App {
  println("Изучаем Cats Effect - книга Essential Effects")

  // https://github.com/inner-product/essential-effects-code

  // В этой книге мы покажем изменения в коде в виде “различий”, которые вы можете увидеть в обзоре кода.
  // Исходный код отображается красным цветом с префиксом -,
  // а обновленная версия - зеленым цветом с префиксом +.

  object part1 {
    def preface = {

      println()
      println("====================================== Preface")

      // Предпосылки
      //Essential Effects основана на общем наборе методов функционального программирования:
      //  функторы, аппликативы и монады. Если какие-либо из них вам не знакомы, пожалуйста, ознакомьтесь с ними ниже.
      //  Возможно, вы не знакомы с самими техническими терминами, но, возможно
      //, вы уже знакомы с концепциями и использовали их в своих проектах.

      // Более глубокое погружение в основы функционального программирования можно найти в книгах
      // Essential Scala
      // https://underscore.io/books/essential-scala/

      // Scala with Cats.
      // https://scalawithcats.com/


      // Functors

      // Функтор отражает понятие чего-либо, что вы можете сопоставить, изменяя его
      // “содержимое” (или выходные данные), но не саму структуру.
      // Многие типы позволяют вам сопоставлять их.
      // Например, все эти типы являются функторами:


      val list = List(1, 2, 3).map(_ + 1)
      val opt = Option(1).map(_ + 1)
      val future = Future(1).map(_ + 1)

      println(list)
      println(opt)
      println{
        Await.result(future, 5 seconds)
      }

      // Сигнатура map для некоторого значения типа F[A], где типом F может быть List, Option и т.д.,
      // выглядит следующим образом:

      // def map[B](f: A => B): F[B]

      // В Essential Effects мы будем использовать map довольно часто.
      // Помимо map, мы также будем использовать методы расширения as и void из Functor:

      val fa: Functor[Option] = Functor[Option]

      val replaced1: Option[String] = fa.map(Some(3))(_ => "replacement")
      val replaced2: Option[String] = fa.as(Some(3),"replacement")

      val voided1: Option[Unit] = fa.map(Some(3))(_ => ())
      val voided2 = fa.void(Some(3))

      println(replaced1, replaced2)
      println(voided1, voided2)

      println("------------------------------")

      // Applicatives

      // Аппликативный функтор, также известный как applicative,
      // это функтор, который может преобразовывать несколько структур, а не только одну.
      // Давайте начнем наш пример с того, что сначала применим map к одному
      // значению параметра (это функтор) и расширим его, чтобы продемонстрировать прикладной
      // метод mapN, действующий на кортежи значений:

      val o1: Option[Int] = Option(1).map(_ + 1)
      val o2: Option[Int] = (Option(1), Option(2)).mapN(_ + _ + 1)
      val o3: Option[Int] =  (Option(1), Option(2), Option(3)).mapN(_ + _ + _ + 1)

      // В более общем плане, для некоторого прикладного типа с именем F[_]
      // мы можем составить кортеж значений F в одно значение F, используя map:

      /*
            def map[B](A => B): F[B]
            def mapN[C]((A,B) => C): F[C]
            def mapN[D]((A,B,C) => D): F[D]

            def mapN[Z]((A, ...) => Z): F[Z]
      */

      // В Essential Effects мы будем использовать аппликативные методы для создания нескольких
      // независимых эффектов, например, при параллельных вычислениях.
      // В частности, мы часто будем использовать символьный аппликативный метод *> для создания двух
      // эффектов, но отбрасывать результат первого. Это эквивалентно следующему вызову mapN:

      val first1 = Option("Один")
      val second1 = Option(1)

      val third1_1: Option[Int] = (first1, second1).mapN((_, b) => b)
      val third1_2: Option[Int] = first1 *> second1

      println(third1_1, third1_2)

      // Метод *> включает в себя два эффекта, первый и второй, с помощью map.
      // Если оба эффекта успешны, мы игнорируем значение первого эффекта,
      // возвращая только значение второго эффекта.

      val first2 = Option.empty[String]
      val second2 = Option(1)
      val third2 =  first2 *> second2

      println {
        third2
      }

      val first3 = Option.empty[String]
      val second3 = Option.empty[Int]
      val third3 =  first3 *> second3

      println {
        third3
      }

      println("------------------------------")

      // Monads

      // Монада - это механизм для упорядочивания вычислений: это выполнение вычисления после этого вычисления.
      // Грубо говоря, монада предоставляет метод flatMap для значения F[A]:

      // def flatMap[B](f: A => F[B]): F[B]

      // Мы можем использовать flatMap некоторой монады F[_] для упорядочивания вычислений:

      def next(a: Int): Option[Int] = Try(10/a).toOption

      val ma1  = Some(5)
      val mb1: Option[Int] = ma1.flatMap(next)
      println(ma1, mb1)

      val ma2  = Some(0)
      val mb2: Option[Int] = ma2.flatMap(next)
      println(ma2, mb2)

      val ma3  = Option.empty[Int]
      val mb3: Option[Int] = ma3.flatMap(next)
      println(ma3, mb3)

      // Производит новое вычисление F[B] на основе (чистого) значения.

      // Поскольку вложенные вызовы flatMap могут усложняться для чтения, когда нам нужно выполнить более двух вычислений,
      // мы можем использовать вместо этого for-comprehension.
      // Это просто синтаксический сахар для вложенных вызовов flatMap:

      def nextB(a: Int): Option[Int] = Try(10/a).toOption
      def nextC(b: Int): Option[String] = Some(b.toString)

      val maa = Option(2)

      val mc1: Option[String] = maa.flatMap{
        a => nextB(a).flatMap{ b =>
          nextC(b)
        }
      }

      val mc2: Option[String] = for {
        a <- maa
        b <- nextB(a)
        c <- nextC(b)
      } yield c

      println(mc1, mc2)


    }

    def chapter1 = {
      println()
      println("====================================== Chapter 1")
    }

  }
    part1.preface
    part1.chapter1


}

