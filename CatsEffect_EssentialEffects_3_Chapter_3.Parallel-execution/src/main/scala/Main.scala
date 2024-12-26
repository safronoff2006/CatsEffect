


import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.implicits.global

import scala.language.postfixOps
import cats.implicits._

import scala.concurrent._
import scala.concurrent.duration._
import utils.debug._


object Main1 extends App {
  println("Изучаем Cats Effect - книга Essential Effects")

  // https://github.com/inner-product/essential-effects-code

  // В этой книге мы покажем изменения в коде в виде “различий”, которые вы можете увидеть в обзоре кода.
  // Исходный код отображается красным цветом с префиксом -,
  // а обновленная версия - зеленым цветом с префиксом +.

  object part1 {
    def chapter3 = {
      // Chapter 3. Parallel execution

      // Захват побочных эффектов в качестве составного типа данных ввода-вывода полезен сам по себе, но как только мы
      // это сделаем, мы сможем продвинуться дальше. Если у нас есть несколько значений ввода-вывода, которые не зависят друг
      // от друга, не должны ли мы иметь возможность запускать их параллельно? Таким образом, наши
      // вычисления в целом могут стать более эффективными.
      // Сначала мы обсудим, поддерживает ли сам IO параллелизм или нет. Затем мы поговорим о том, как
      // IO может поддерживать параллелизм и как этот параллелизм реализуется. Затем мы
      // рассмотрим несколько примеров различных способов параллельного создания значений ввода-вывода.

      // 3.1 Поддерживает ли IO параллелизм?

      // Чтобы ответить на вопрос о том, поддерживает ли IO параллелизм, давайте сначала
      // сравним его с аналогичным типом данных scala.concurrent.Future, который, как мы видели, поддерживает
      // параллелизм , планируя работу в нескольких потоках с помощью
      // scala.concurrent.ExecutionContext.

      //Давайте композируем несколько Future, используя как flatMap (с помощью for-comprehension), так и map.
      // В приведенном ниже коде эффект hw1 такой же, как и эффект hw2?
      // Работают ли hello и world параллельно или нет? Какие выходные данные мы увидим на консоли?

      implicit val ec = ExecutionContext.global

      val hello = Future{
        println(s"[${Thread.currentThread.getName}] Hello")
        Thread.sleep(10)
      }
      val world = Future{
        println(s"[${Thread.currentThread.getName}] World")
        Thread.sleep(10)
      }

      val hw1: Future[Unit] = for {
        _ <- hello
        _ <- world
      } yield ()

      val res1 = Await.ready(hw1, 5 seconds)

      val hw2: Future[Unit] = (hello, world).mapN((_, _) => ())
      val res2 = Await.ready(hw2, 5 seconds)

      // Мы добавили некоторый вспомогательный код, чтобы показать текущий поток во время
      // выполнения Future.

      // Эффект hw1 такой же, как...
      //.. эффект hw2?

      // Если мы запустим эту программу, то увидим следующий результат
      // (ваши идентификаторы потоков могут отличаться).:

      // [scala-execution-context-global-21] Hello
      // [scala-execution-context-global-22] World

      // Мы видим только одну пару напечатанных надписей "Привет" и "Мир". Почему?
      // Потому что Future автоматически планирует действие и кэширует результат.
      // Это нарушает правило № 2 нашего эффекта
      // Шаблон: небезопасный побочный эффект не выполняется отдельно от функций, подобных конструктору Future.
      // Прости! Мы сочли необходимым напомнить вам, что Future с готовностью применяет побочные эффекты.

      println("-----------------")

      // Давайте постараемся отсрочить побочные эффекты как можно дольше:

      def helloF = Future{
        println(s"[${Thread.currentThread.getName}] Hello")
        Thread.sleep(10)
      }
      def worldF = Future{
        println(s"[${Thread.currentThread.getName}] World")
        Thread.sleep(10)
      }
      val hw1F: Future[Unit] = for {
          _ <- helloF
          _ <- worldF
      } yield ()

      val res1F = Await.ready(hw1, 5 seconds)

      val hw2F: Future[Unit] = (helloF, worldF).mapN((_, _) => ())
      val res2F = Await.ready(hw2, 5.seconds)

      // Обратите внимание, что мы заменили значение val на значение def, чтобы избежать кэширования в режиме Future из предыдущего примера.

      // Является ли эффект hw1 таким же, как...

      //... эффект hw2?

      // Если мы запустим эту программу, то увидим следующий результат:

        // [scala-execution-context-global-21] Hello
        // [scala-execution-context-global-23] Hello
        // [scala-execution-context-global-22] World
        // [scala-execution-context-global-33] World

      // Теперь вывод — корректный — показывает нам две пары выходных данных Hello и World.
      // И мы видим, что вторая пара выходных данных от вызова map выполняется в разных потоках.
      // Но будьте осторожны! Даже если мы видим, что вывод выполняется в двух разных потоках,
      // это не означает, что эти вычисления выполнялись параллельно.
      // Как вы могли бы показать, выполнялись они параллельно или нет? (Не так уж важно отвечать на этот вопрос).
      // В то же время, вычисление hw2 на самом деле недетерминировано, поэтому вы также можете
      // увидеть Hello, напечатанный перед World. Что делает выходные данные недетерминированными?

      // В то время как эффекты hw2 недетерминированы, мы можем сказать одно о
      // выполнении hw1, вычислении, которое использует for-comprehension.
      // Поскольку функция for-comprehension является синтаксическим дополнением для серии вложенных вызовов flatMap,
      // мы можем с уверенностью сказать, что эффект world всегда будет выполняться после эффекта hello,
      // поскольку эффект world создается (и впоследствии выполняется) только после вычисления значения
      // hello:

      /*
              val hw1: Future[Unit] =
              - for {
              -    _ <- hello
              -    _ <- world
              - } yield ()
              + hello.flatMap { _ =>
              +   world.map { _ =>
              +     ()
              +   }
              + }
      */

      // Синтаксический сахар for-comprehension - это серия вложенных вызовов flatMap.
      // Как только мы разберем синтаксис, мы увидим, что world выполняется только после
      // вычисления результата hello, потому что результат hello передается функции, заданной для
      // flatMap.

      // С другой стороны, мы можем сделать вывод, что hw2, использующий map, будет запускать hello и
      // world параллельно. Почему это так?
      // Если мы немного проанализируем определение hw2, то сможем увидеть ,
      // что происходит нечто важное:

     /*
           val hw2: Future[Unit] =
              (
            -   hello,
            +   Future(println(s"[${Thread.currentThread.getName}] Hello")) 1
            -   world
            +   Future(println(s"[${Thread.currentThread.getName}] World")) 2
              ).mapN((_, _) => ())
     */

      // Мы знаем, что когда мы создаем Future, оно становится запланированным.
      // Второе Future также запланировано, так что теперь оба будут выполняться независимо.

      // Это демонстрирует, что для Future flatMap и map имеют разные эффекты в отношении параллелизма.
      // Но обратите внимание: это не тот случай, когда mapN для Future реализован с использованием параллелизма,
      // а flatMap реализован как нечто последовательное.
      // Параллелизм возникает как побочный эффект — каламбур — от того, что Future с готовностью планирует вычисления,
      // которые происходят до того, как будет вычислена сама map.

      println("---------------")

      // Что насчет IO? Имеет ли использование map и flatMap другой эффект, как это делает Future?

      val helloIO = IO(println(s"[${Thread.currentThread.getName}] Hello"))
      val worldIO = IO(println(s"[${Thread.currentThread.getName}] World"))

      val hw1IO: IO[Unit] = for {
          _ <- helloIO
          _ <- worldIO
        } yield ()

      val hw2IO: IO[Unit] = (helloIO, worldIO).mapN((_, _) => ())

      hw1IO.unsafeRunSync()
      hw2IO.unsafeRunSync()

      // Мы используем код, эквивалентный предыдущим примерам, с Future, чтобы
      // обеспечить максимальную согласованность.

      // Является ли эффект hw1 таким же, как...

      //... эффект hw2?

      // вывод программы
        // [io-compute-2] Hello
        // [io-compute-2] World
        // [io-compute-0] Hello
        // [io-compute-0] World

      // Теперь мы видим ожидаемый результат, но все потоки одинаковы. Ожидали ли вы этого?
      // Считаете ли вы, что hw2 имеет недетерминированный результат, как в предыдущем примере с использованием Future?
      // IO не обеспечивает никакой поддержки эффекта параллелизма! И это сделано специально,
      // потому что мы хотим, чтобы разные эффекты имели разные типы в соответствии с нашим шаблоном эффектов.

      // 3.2. The Parallel typeclass

      // Как мы уже видели, в отличие от Future, IO сам по себе не поддерживает параллелизм.
      // Итак, как мы можем этого добиться?
      // Мы снова будем следовать нашему шаблону эффектов и применим правило № 1: тип должен отражать эффект.
      // Если IO не поддерживает параллелизм, нам нужен новый тип, который поддерживает.
      // В cats.effect этот тип называется IO.Par (Par означает “параллельный”).

      /*
              sealed abstract class IO[+A] { ... } 1

              object IO {
                class Par[+A] { ... } 2

                object Par {
                  def apply[A](ioa: IO[A]): Par[A] = ??? 3
                  def unwrap[A](pa: Par[A]): IO[A] = ??? 3
                }
              }
      */

      // 1 Тип данных (последовательного) IO
      // 2 Тип данных параллельного  IO, IO.Par
      // 3 Методы преобразования между IO и IO.Par значениями

      // IO.Par не является экземпляром Monad, потому что мы не хотим, чтобы можно было сериализовать последовательно
      // выполнение нескольких действий. Вместо этого у него будет экземпляр Applicative для
      // создания независимого IO.Par значения

      /*
            implicit def ap(implicit cs: ContextShift[IO]): Applicative[IO.Par] = 1
              new Applicative[IO.Par] {
                def pure[A](a: A): IO.Par[A] = IO.Par(IO.pure(a))
                def map[A, B](pa: IO.Par[A])(f: A => B): IO.Par[B] = ???
                def product[A, B](pa: IO.Par[A], pb: IO.Par[B]): IO.Par[(A, B)] = ??? 2
              }
      */

      // 1 Нам требуется Context Shift[IO], чтобы иметь возможность переключать вычисления в разные потоки.
      // Мы подробнее поговорим о ContextShift в главе 5 "Изменение контекстов", но пока вы можете думать
      // о нем как о чем-то подобном scala.concurrent.ExecutionContext или пуле потоков.
      // 2 Реализация product обеспечит выполнение pa и pb в разных потоках с использованием cs.

      // Необходимость переключать типы при переводе между последовательным и параллельным выполнением
      // является несколько многословной. Это выглядело бы так:

      /*
            val ia: IO[A] = IO(???)
            val ib: IO[B] = IO(???)

            def f(a: A, b: B): C = ???

            val ipa: IO.Par[A] = IO.Par(ia) 1
            val ipb: IO.Par[B] = IO.Par(ib) 1

            val ipc: IO.Par[C] = (ipa, ipb).mapN(f) 2

            val ic: IO[C] = IO.Par.unwrap(ipc) 3
      */

      // Parallel класс типов из библиотеки Cats (не Cats Effect) отражает концепцию преобразования между
      // двумя связанными типами данных:

      /*
              trait Parallel[S[_]] {    1
                type P[_]      2

                def monad: Monad[S]      3

                def applicative: Applicative[P]      4

                def sequential: P ~> S     5

                def parallel: S ~> P       5
              }
      */

      // 1 Экземпляры Typeclass относятся к типам S (для последовательных).
      // Например, будет существовать экземпляр typeclass Parallel[IO], где IO - это
      // преобразуемый последовательный тип.
      // 2 Экземпляр Typeclass определяет тип P (для параллельных). Для параллельного экземпляра Parallel[IO]
      //  typeclass, P будет IO.Par.
      // 3 S должны содержать монаду. То есть операции с использованием S должны быть упорядочены.
      // 4 P должны иметь значение Applicative. То есть операции, использующие P, не должны иметь каких
      //  -либо зависимостей от порядка данных.
      // 5 Параллельный экземпляр должен быть способен преобразовывать последовательные значения в параллельные и обратно.

      // Символ ~> является псевдонимом типа для cats.arrow.FunctionK, которая представляет собой
      // преобразование из некоторого типа F[A] в другой тип G[A] для любого типа A.
      // Таким образом, тип P ~> S эквивалентен коду типа
      // def apply[A](pa: P[A]): S[A].

      // Диаграмма: Parallel typeclass кодирует преобразования между последовательным типом S и параллельным типом P.
      // resources/Parallel_typeclass.png

      //Переписываем трансляцию между IO и IO.Par в терминах Parallel, который теперь у нас есть:

      /*
              val ia: IO[A] = IO(???)
              val ib: IO[B] = IO(???)

              def f(a: A, b: B): C = ???

              - val ipa: IO.Par[A] = IO.Par(ia)
              - val ipb: IO.Par[B] = IO.Par(ib)
              + val ipa: IO.Par[A] = Parallel[IO].parallel(ia)
              + val ipb: IO.Par[B] = Parallel[IO].parallel(ib)

              val ipc: IO.Par[C] = (ipa, ipb).mapN(f)

              - val ic: IO[C] = IO.Par.unwrap(ipc)
              + val ic: IO[C] = Parallel[IO].sequential(ipc)
      */

      // Однако мы можем сделать лучше. Как только определен экземпляр класса параллельного типа,
      // для последовательного типа становятся доступны версии функций с префиксом par,
      // которые автоматически выполняют это преобразование, поэтому
      // вы никогда не увидите изменения типа, лежащего в основе:

      /*
              val ia: IO[A] = IO(???)
              val ib: IO[B] = IO(???)

              def f(a: A, b: B): C = ???

              - val ipa: IO.Par[A] = Parallel[IO].parallel(ia)
              - val ipb: IO.Par[B] = Parallel[IO].parallel(ib)
              -
              - val ipc: IO.Par[C] = (ipa, ipb).mapN(f)
              -
              + val ic: IO[C] = Parallel[IO].sequential(ipc)
              + val ic: IO[C] = (ia, ib).parMapN(f) 1
      */

      // 1 Обратите внимание на префикс par!

      // parMapN преобразует аргументы ia и ib в значения IO.Par,
      // композируя их параллельно с помощью IO.Par и mapN, и преобразует результаты обратно в IO.

      // Посмотрите, сколько кода мы сэкономили, абстрагировавшись от этого общего шаблона!
      // Диаграмма:
      // resources/parMapN.png

      // Метод расширения parMapN реализуется как
      // (1) преобразование последовательных типов эффектов в параллельные представления,
      // (2) выполнение альтернативной map и
      // (3) преобразование параллельного представления обратно в последовательную форму.

      // 3.3. Проверка параллелизма

      // parMapN и другие методы, доступные в классе Parallel type, эффективно абстрагируют параллелизм,
      // устраняя детали того, как на самом деле выполняются эти вычисления.
      // Но когда мы учимся их использовать, как мы получаем представление о том, что выполняется и как?
      // Самым простым решением было бы вывести что-либо на консоль во время выполнения.
      // Мы знаем, что это побочный эффект, и мы знаем, что делать с побочными эффектами:
      // обернуть их в IO!
      // Мы создали вспомогательный метод debug, который добавим в наш код. Просто добавьте этот импорт:

      /*

      */

      //Смотрите и запускайте пример DebugExample ниже

      // Во время выполнения метод debug выведет имя текущего потока вместе со значением, полученным в результате эффекта
      // (в виде строки, полученной при вызове toString):

        // [io-compute-11] hello
        // [io-compute-11] world
        // [io-compute-11] hello world


      // Выполнение действия seq с использованием map.
      // Из названий потоков мы видим, что оно выполняется полностью в том же потоке.

      // Исходный код для отладки очень прост — он создает новый эффект, который выводит значение
      // данного эффекта на консоль вместе с названием текущего потока

      // Мы используем другой вспомогательный инструмент, чтобы в терминале названия потоков были выделены
      // красивыми цветами, чтобы сделать их более визуально различимыми.

      println("------------")

      // 3.4. parMapN

      // parMapN - это параллельная версия метода applicative map. Он позволяет нам параллельно объединять
      // несколько эффектов в один, указывая, как комбинировать выходные данные эффектов:

      /*
            val ia: IO[A] = IO(???)
            val ib: IO[B] = IO(???)

            def f(a: A, b: B): C = ???

            val ic: IO[C] = (ia, ib).parMapN(f)
      */

      // mapN и parMapN воздействуют на кортежи любой арности, поэтому мы можем
      // согласованно комбинировать любое количество эффектов.
      // Например:

      /*
              (ia, ib).parMapN((a, b) => ???) 1

              (ia, ib, ic).parMapN((a, b, c) => ???) 2

              (ia, ib, ic, id).parMapN((a, b, c, d) => ???) 3

              1 Two effects → one effect.
              2 Three effects → one effect.
              3 Four effects → one effect.
      */

      // Давайте создадим пример приложения, использующего parMapN, вместе с нашей отладкой,
      // чтобы мы могли видеть, что происходит.

      val hello2: IO[String] = IO("hello").debugio

      val world2: IO[String] = IO("world").debugio

      val par: IO[Unit] = (hello2, world2)
                  .parMapN((h, w) => s"$h $w")
                  .debugio
                  .void

      par.unsafeRunSync()

      // 1 Мы отлаживаем каждое значение IO, которое будет выполняться (параллельно).
      // 2 В предыдущем примере мы используем parMapN вместо mapN.
      // 3 Мы также отлаживаем составленное значение IO. Как вы думаете, что будет напечатано?
      //  Как вы думаете, в каком потоке он будет работать?

      // Running the ParMapN program produces:
        // [io-compute-7] hello
        // [io-compute-6] world
        // [io-compute-6] hello world

      // Выполнение действия задач с помощью parMapN. Обратите внимание на различные используемые потоки!

      // Порядок выполнения параллельных задач недетерминирован, поэтому при запуске программы вы можете увидеть,
      // что hello и world выводятся в другом порядке.

      println("--------------------")

      // 3.4.1. поведение parMapN при наличии ошибок

      // Вот программа, которая выводит выходные данные трех эффектов, составленных из parMapN,
      // каждый из которых, представляет собой комбинацию эффектов успеха и неудачи.
      // Что произойдет, если один (или более) из входных эффектов приведет к ошибке?
      // Какое значение возвращается?
      // Является ли оно детерминированным?

      // Смотрите и запускайте пример ParMapNErrors ниже.

      // Вызов attempt преобразует IO[A] в IO[Either[Throwable, A]],
      // гарантируя, что эффект всегда будет успешным (но с оставленным значением, если он на самом деле не удался).
      // Мы используем attempt, чтобы гарантировать, что наши эксперименты с ошибками не остановят программу.

      // Running ParMapNErrors outputs:

        // [io-compute-8] hi                                       1
        // [io-compute-6] Left(java.lang.RuntimeException: oh!)    1
        // [io-compute-6] ---
        // [io-compute-6] hi                                       2
        // [io-compute-8] Left(java.lang.RuntimeException: oh!)    2
        // [io-compute-8] ---
        // [io-compute-5] Left(java.lang.RuntimeException: oh!)    3

      // Все три эффекта приводят к Left (отказу).

      // Давайте сначала опишем, что должно быть верно для всех этих эффектов:
      // результат parMapN завершится ошибкой, если какой—либо — хотя бы один - из эффектов завершится ошибкой.
      // И мы видим, что на выходе получены три Left значения, соответствующие e1, e2 и e3.
      // В то же время мы знаем, что каждый дополнительный эффект (ok, ko1, ko2) выполняется параллельно.

      // Учитывая эти условия, для эффекта e1 мы видим результат ok, что означает , ч
      // то ok сработал до ko1. Мы не видим результат ok от e2, поэтому можем предположить,
      // что эффект e2 ko1 сработал первым.
      // Как эффект e1 ok, так и эффект e2 ko1 являются первыми аргументами parMapN.
      // Означает ли это, что крайние левые аргументы parMapN всегда будут выполняться первыми?
      // Не обязательно! Рассмотрим, не следует ли отложить выполнение ko1 на время ожидания:

      // Смотрите и запускайте пример ParMapNErrorsDelayed

      // ParMapNErrorsDelayed then outputs:

        // [io-compute-7] hi
        // [io-compute-9] ko1
        // [io-compute-9] Left(java.lang.RuntimeException: oh!)
        // [io-compute-9] ---
        // [io-compute-9] hi
        // [io-compute-7] ko1
        // [io-compute-7] Left(java.lang.RuntimeException: oh!)
        // [io-compute-7] ---
        // [io-compute-4] Left(java.lang.RuntimeException: noes!)

      // 1 Мы отложили ko1, поэтому для e1 мы видим выходные данные ok и ko1 до того, как ko1 вызовет исключение.
      // 2 Для e2, несмотря на то, что ko1 является первым аргументом parMapN, мы видим тот же результат, что и для e1.
      // 3 Для e2 мы видим результат ko2, поскольку ko1 был отложен и, следовательно, выполнен после ko2.

      // Что произойдет, если во время parMapN возникнут сбои? Первый сбой, который произойдет,
      // используется как сбой созданного эффекта.

      // 3.4.2. parTupled

      // Код parMapN((_, _) => ()) выглядит немного некрасиво. С этим выражением мы делаем две вещи:

      // 1. Независимо от того, каковы Результаты воздействия входных данных, мы хотим создать Unit.
      // 2. Нам все равно, каковы будут два результата входных эффектов, мы “называем” их _, чтобы игнорировать их.

      //Для достижения первой цели мы могли бы использовать комбинатор void, который определяется как
      // map(_=> ()):

      /*
            - val e1 = (ok, ko1).parMapN(???).map(_ => ())
            + val e1 = (ok, ko1).parMapN(???).void
      */

      // Но что мы можем использовать вместо ??? Самая простая функция, которую мы можем передать в map - функцию,
      // которая делает пару:

      /*
            val e1 = (ok, ko1).parMapN((l, r) => (l, r)).void
      */

      // cats предоставляет функцию (par-)mapN, которая ничего не делает, кроме
      // объединения входных данных в кортеж, называемую (par-)tupled:

      /*
      (ia, ib).parTupled 1
      (ia, ib, ic).parTupled 2
      (ia, ib, ic, id).parTupled 3
                               ... 4

      1 Two IO → one IO of a Tuple2: (IO[A], IO[B]) ⇒ IO[(A, B)]
      2 Three IO → one IO of a Tuple3: (IO[A], IO[B], IO[C]) ⇒ IO[(A, B, C)]
      3 Four IO → one IO of a Tuple4: (IO[A], IO[B], IO[C], IO[D]) ⇒ IO[(A, B, C, D)]
      4 And so on.

      */

      // Таким образом, наши примеры обработки ошибок, приведенные выше, можно было бы записать:

      /*
          - val e1 = (ok, ko1).parMapN((l, r) => (l, r)).void
          + val e1 = (ok, ko1).parTupled.void
      */

      // Смотрите и запускайте пример ParMapNErrorsParTupledVoid

      // 3.5. parTraverse

      // parTraverse - это параллельная версия traverse; обе версии имеют одинаковую сигнатуру типа:

      /*
              F[A] => (A => G[B]) => G[F[B]]
      */

      // Например, если F - это список, а G - это IO, то (par)traverse будет функцией от
      // List[A] к IO[List[B]], если задана функция A => IO[B].

      /*
              List[A] => (A => IO[B]) => IO[List[B]]
      */

      // Наиболее распространенный вариант использования (par)traverse - это когда у вас есть набор работ,
      // которые необходимо выполнить, и функция, которая обрабатывает одну единицу работы.
      // Тогда вы получаете набор результатов, объединенных в один эффект:

      sealed trait WorkUnit
      case class Work(n: Int) extends WorkUnit

      sealed trait Result
      case class IntResult(n: Int) extends Result

      val work: List[WorkUnit] = List(Work(1),Work(2), Work(3))
      def doWork(workUnit: WorkUnit): IO[Result] = (workUnit match {
        case Work(n) => IO(IntResult(n * n))
        case _ => IO(IntResult(0))
      }).debugio
      val results: IO[List[Result]] = work.parTraverse(doWork)

      results
        .debugio
        .void
        .unsafeRunSync()

      // Обратите внимание, что обработка одной единицы работы - это эффект, в данном случае IO.
      // Давайте воспользуемся нашим отладочным комбинатором, чтобы лучше видеть выполнение при использовании parTraverse.

      // Посмотрите ниже и выполните пример ParTraverse

      // 1 Мы разбиваем задачи на части: каждый Int из задач преобразуется в IO[Int] с помощью метода task,
      // и они выполняются параллельно.
      // 2 Мы используем debug combinator для выполнения каждой задачи и для получения конечного результата.

      // Запуск программы ParTraverse приводит к:

        // [ioapp-compute-7] 7
        // [ioapp-compute-0] 0
        // [ioapp-compute-6] 6
        // [ioapp-compute-1] 1
        // [ioapp-compute-2] 2
        // [ioapp-compute-5] 5
        // [ioapp-compute-4] 4
        // [ioapp-compute-3] 3
        // [ioapp-compute-7] 8
        // [ioapp-compute-3] 13
        // ...
        // [ioapp-compute-4] List(0, 1, 2, ... 98, 99)

      // Если все результаты вычисляются параллельно, как создается List[B] результатов с помощью
      // возвращаемого IO[List[B]]?
      // Получение результата типа IO[List[B]] должно означать, что возвращаемый IO должен был
      // собрать все результаты — List[B] — даже если каждый B был вычислен независимо.

      // Необходимо дождаться, пока будут пройдены все элементы, но возвращаемый List[B] можно создавать постепенно,
      // ожидая вычисления первого результата, затем добавляя второй результат, когда он будет вычислен, и так далее.

      // При этом parTraverse на самом деле написан в терминах traverse, где он преобразует каждый ввод-вывод в IO.Par.
      // Поскольку для прохождения требуется только, чтобы эффект имел Аппликативный экземпляр,
      // Applicative[IO.Par] - это место, где “происходит” параллелизм.

      // 3.5.1. Другой взгляд на parTraverse

      // Вы также можете рассматривать (par)traverse как разновидность (paк)mapN, где результаты собираются,
      // но где каждый выходной эффект имеет один и тот же тип выхода:

      def f(i: Int): IO[Int] = IO(i)

      val iol1 =             (f(1),f(2)).parMapN((a, b) => List(a, b))               // IO[List[Int]]     1
      val iol2 =      (f(1), f(2), f(3)).parMapN((a, b, c) => List(a, b, c))         // IO[List[Int]]     2
      val iol3 = (f(1), f(2), f(3),f(4)).parMapN((a, b, c, d) => List(a, b, c, d))   // IO[List[Int]]     3
      val iol4 =        List(1, 2, 3, 4).parTraverse(f)                              // IO[List[Int]]     4

      // 1 Мы вычисляем f(1), f(2) и собираем результаты в список.
      // 2 Мы вычисляем f(1), f(2), f(3) и собираем результаты в список.
      // 3 Мы вычисляем f(1), f(2), f(3), f(4) и собираем результаты в список.
      // 4 Список(1, 2, 3, 4).parTraverse(f) тоже самое что
      // (f(1), f(2), f(3), f(4)).parMapN(...).

      // Обратите внимание, что тип возвращаемого значения для всех этих выражений один и тот же: IO[List[Int]].

      // 3.6. parSequence

      // (par)sequence выворачивает вложенную структуру “наизнанку”:

     /*
              F[G[A]] => G[F[A]]
     */

      // Например, если у вас есть список эффектов IO, parSequence параллельно преобразует
      // его в один эффект IO, который создает список выходных данных:

      /*
              List[IO[A]] => IO[List[A]]
      */

      // Давайте посмотрим на parSequence в действии:
      // Смотрите и запускайте пример ParSequence ниже

      // 1 Мы используем parSequence для задач: каждый ввод-вывод задач выполняется параллельно.
      // 2 Мы используем комбинатор отладки для каждого эффекта задачи и эффекта конечного результата.

      // Running the ParSequence program produces:
        // [ioapp-compute-2] 2
        // [ioapp-compute-4] 4
        // [ioapp-compute-1] 1
        // [ioapp-compute-7] 7
        // [ioapp-compute-3] 3
        // [ioapp-compute-0] 0
        // [ioapp-compute-2] 8
        // [ioapp-compute-6] 6
        // [ioapp-compute-1] 12
        // ...
        // [ioapp-compute-6] List(0, 1, 2, ... 98, 99)

      // Обратите внимание, что sequence и traverse взаимозаменяемы:
      //  x.sequence - это x.traverse(identity), а
      //  x.traverse(f) - это x.map(f).sequence.

      // 3.7. Резюме

      // Сам по себе, IO не поддерживает параллельные операции, поскольку является монадой.
      // 2. Тайп класс Parallel  определяет преобразование между парой типов эффектов:
      // один из которых является монадой, а другой - “всего лишь” аппликативным функтором.
      // 3. Parallel[IO] связывает эффект IO с его параллельным аналогом, IO.Par.
      // 4. Параллельная структура IO требует возможности переноса вычислений в другие потоки в рамках текущего контекста выполнения.
      // Именно так “реализуется” параллелизм.
      // 5. parMapN, parTraverse, parSequence являются параллельными версиями (последовательного) map, traverse, and sequenc.
      // Ошибки устраняются способом fail-fast.

      // При проектировании систем fail-fast — это система,
      // которая немедленно сообщает на своем интерфейсе о любом состоянии,
      // которое может указывать на сбой.
    }
  }

  part1.chapter3

}

object DebugExample extends IOApp {

  def run(args: List[String]): IO[ExitCode] = seq.as(ExitCode.Success)

  // Uses debugio to add console output during execution.

  private val hello: IO[String] = IO("hello").debugio
  private val world: IO[String] = IO("world").debugio

  private val seq: IO[String] =
    (hello, world)
      .mapN((h, w) => s"$h $w")
      .debugio
}


object ParMapNErrors extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    e1.attempt.debugio *>
    IO("---").debugio *>
    e2.attempt.debugio *>
    IO("---").debugio *>
    e3.attempt.debugio *>
    IO.pure(ExitCode.Success)

  private val ok = IO("hi").debugio
  private val ko1 = IO.raiseError[String](new RuntimeException("oh!")).debugio
  private val ko2 = IO.raiseError[String](new RuntimeException("noes!")).debugio
  private val e1 = (ok, ko1).parMapN((_, _) => ())
  private val e2 = (ko1, ok).parMapN((_, _) => ())
  private val e3 = (ko1, ko2).parMapN((_, _) => ())

}

object ParMapNErrorsDelayed extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    e1.attempt.debugio *>
      IO("---").debugio *>
      e2.attempt.debugio *>
      IO("---").debugio *>
      e3.attempt.debugio *>
      IO.pure(ExitCode.Success)

  private val ok = IO("hi").debugio
  private val ko1 = IO.sleep(1.second).as("ko1").debugio *> IO.raiseError[String](new RuntimeException("oh!")).debugio
  private val ko2 = IO.raiseError[String](new RuntimeException("noes!")).debugio
  private val e1 = (ok, ko1).parMapN((_, _) => ())
  private val e2 = (ko1, ok).parMapN((_, _) => ())
  private val e3 = (ko1, ko2).parMapN((_, _) => ())

}

object ParMapNErrorsParTupledVoid extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    e1.attempt.debugio *>
      IO("---").debugio *>
      e2.attempt.debugio *>
      IO("---").debugio *>
      e3.attempt.debugio *>
      IO.pure(ExitCode.Success)

  private val ok = IO("hi").debugio
  private val ko1 = IO.sleep(1.second).as("ko1").debugio *> IO.raiseError[String](new RuntimeException("oh!")).debugio
  private val ko2 = IO.raiseError[String](new RuntimeException("noes!")).debugio
  private val e1 = (ok, ko1).parTupled.void
  private val e2 = (ko1, ok).parTupled.void
  private val e3 = (ko1, ko2).parTupled.void

}

object ParTraverse extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    tasks
      .parTraverse(task)
      .debugio
      .as(ExitCode.Success)

  val numTasks = 100
  val tasks: List[Int] = List.range(0, numTasks)

  def task(id: Int): IO[Int] = IO(id).debugio
}

object ParSequence extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    tasks.parSequence
    .debugio
    .as(ExitCode.Success)
  val numTasks = 100
  val tasks: List[IO[Int]] = List.tabulate(numTasks)(task)

  def task(id: Int): IO[Int] = IO(id).debugio
}

// К каждой из больших глав книги есть СНОСКИ,
// Я их проанализирую и прокомментирую и как то дам ссылки на эти сноски в коде этих примеров...