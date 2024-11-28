


import cats.effect.{Deferred, IO}
import cats.effect.kernel.Ref
import cats.effect.std.{CountDownLatch, CyclicBarrier, Semaphore}
import cats.effect.unsafe.implicits.global
import cats.implicits.toTraverseOps

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
      // Синхронизация и конкурентный доступ в Cats Effect 3 [Часть 7]

      // Введение
      // Это часть 7 серии блогов Cats Effect 3. В этой части мы рассмотрим примитивы синхронизации, доступные в CE3.
      // https://yadukrishnan.live/series/cats-effect

      // Синхронизация и параллелизм
      // Когда у нас есть несколько волокон/потоков, работающих с одними и теми же наборами данных,
      // становится важным иметь безопасный параллельный доступ и синхронизацию с общими состояниями.
      // Cats Effect предоставляет широкий спектр опций для обработки таких случаев.

      // Ref
      // Ref обеспечивает потокобезопасный параллельный доступ и изменение общих состояний.
      // Под капотом он использует Java AtomicReference,
      // но предоставляет функциональный способ обработки.
      // Давайте рассмотрим, как создавать и изменять состояния с помощью Ref.

      // В основном мы можем использовать 2 разных способа создания Ref:

      val mutableState: IO[Ref[IO, Int]] = Ref[IO].of(100)
      val mutableStateV2: IO[Ref[IO, Int]] = IO.ref(100)

      // Мы можем использовать метод get для получения значения из Ref.
      // Аналогично мы можем использовать метод set для установки значения переменной Ref.
      // Метод getAndSet возвращает текущее значение, а также устанавливает новое значение для Ref.
      // Давайте рассмотрим эти методы в действии:

      val refOps: IO[Unit] = for {
        ref <- IO.ref(0) //initialises a Ref
        value <- ref.get.trace // returns 0
        _ <- ref.set(1) //sets the ref with value 1
        oldValue <- ref.getAndSet(5).trace //returns 1 and set 5
        _ <- ref.get.trace //returns 5
      } yield ()

      refOps.unsafeRunSync()

      println("-----------")

      // CE также предоставляет некоторые дополнительные методы, такие как update, updateAndGet и getAndUpdate.
      // Они похожи на методы get и set, но позволяют передавать функцию для изменения состояния:

      val refWithFns: IO[Unit] = for {
        ref <- IO.ref("Hello World")
        cap <- ref.getAndUpdate(_.toUpperCase).trace //returns str and then make ref upper case
        str <- ref.get.trace // prints str in uppercase
        _ <- ref.update(_.toLowerCase) //update the state
        strV2 <- ref.get.trace // prints str in all lower
        firstWord <- ref.updateAndGet(_.split("\\s").head.toUpperCase()).trace
      } yield ()

      refWithFns.unsafeRunSync()

      println("-----------")

      // Существует метод, называемый modify, который делает то же самое, что и update.
      // Но modify позволяет возвращать другой тип возвращаемого значения после обновления состояния.
      // Например, мы можем использовать ссылку из предыдущего примера и посмотреть, как использовать modify :

      val refWithModify: IO[Unit] = for {
        ref <- IO.ref("Hello World")
        currentStr <- ref.get.trace // prints Hello World
        _ = println("Length of current string is :" + currentStr.length) // returns 11
        len <- ref.modify(value => (value.toUpperCase + "!", value.length)).trace //similar to getAndUpdate, but returns length
        newStr <- ref.get.trace //prints HELLO WORLD!
        _ = println("Length of updated string is: " + newStr.length) //returns 12
      } yield ()

      refWithModify.unsafeRunSync()

      println("-----------")

      // Deferred

      // Deferred — это нечто похожее на концепцию Promise. Он хранит значение, которое еще не доступно.
      // При создании Deferred он будет пустым. Затем его можно будет завершить один раз со значением.

      //  Он имеет 2 важных метода, get и complete.
      //  Метод get блокируется (семантически) до тех пор, пока значение не станет доступно в отложенной переменной.
      //  Метод complete используется для завершения отложенной переменной, и все волокна, вызвавшие метод get,
      //  будут уведомлены о готовности значения.
      //  Мы можем создать отложенную переменную, используя метод deferred на IO.
      //  Давайте создадим программу для приготовления кофе с использованием Deferred:

      def developer(coffeeSignal: Deferred[IO, String]): IO[Unit] = for {
        _ <- IO("Developer wants to drink coffee and waiting for it to be ready").trace
        _ <- coffeeSignal.get.trace // impatiantly waiting on coffee machine for it to be prepared
        _ <- IO("Started sipping the divine coffee.. ").trace
      } yield ()

      def coffeeMachine(coffeeSignal: Deferred[IO, String]): IO[Unit] = for {
        _ <- IO("Verifying water and coffee beans").trace
        grindingFib <- (IO("Grinding Coffee Beans").trace >> IO.sleep(Random.nextInt(500).millis) >> IO("Grinding complete").trace ).trace.start
        boilingFib <- (IO("Boiling Water").trace >> IO.sleep(Random.nextInt(500).millis) >> IO("Boiling complete").trace).start
        _ <- grindingFib.join
        _ <- boilingFib.join
        _ <- IO("Adding milk").trace
        _ <- IO.sleep(100.millis)
        _ <- coffeeSignal.complete("Coffee Ready!")
      } yield ()

      def makeCoffee: IO[Unit] = for {
        coffeeSignal <- IO.deferred[String]
        fib1 <- developer(coffeeSignal).start
        fib2 <- coffeeMachine(coffeeSignal).start
        _ <- fib1.join
        _ <- fib2.join
      } yield ()

      makeCoffee.unsafeRunSync()

      println("-----------")

      // Семафор

      // Семафор — это концепция синхронизации параллельности, в которой мы можем ограничить количество потоков,
      // которые могут получить доступ к некоторому ресурсу одновременно.
      // Семафор содержит положительное количество разрешений.
      // Любые волокна/потоки, которым необходимо получить доступ к защищенному ресурсу, должны получить разрешение.
      // После того, как доступ будет выполнен, разрешение будет снято.
      // Если доступные разрешения равны нулю, все входящие волокна будут семантически заблокированы,
      // пока что-то не станет доступным.

      // Семафор определяет такие методы, как acquire, release, acquireN и releaseN.

      // Давайте попробуем реализовать сценарий ванной комнаты с семафорами.
      // Предположим, что есть только 2 туалета.
      // Если более 2 человек должны одновременно зайти в туалет, некоторым из них придется ждать,
      // пока остальные закончат и выйдут:

      def currentTime: Long = System.nanoTime

      def accessWashroom(person: String, sem: Semaphore[IO]): IO[Unit] = for {
        _ <- IO(s"[$currentTime] $person wants to access the washroom, waiting for getting the access").trace
        _ <- sem.acquire
        _ <- IO(s"[$currentTime] $person got access to washroom and using it now").trace
        _ <- IO.sleep(5.second)
        _ <- IO(s"[$currentTime] $person left the washroom").trace
        _ <- sem.release
      } yield ()

      val persons: List[String] = (1 to 5).map("Person-" + _).toList
      val washroomAccessPgm: IO[Unit] = for {
        washrooms <- Semaphore[IO](2)
        fibers <- persons.map(p => accessWashroom(p, washrooms).start).sequence
        _ <- fibers.map(_.join).sequence
      } yield ()

      washroomAccessPgm.unsafeRunSync()

      // Если мы запустим washroomAccessPgm, то увидим, что некоторым людям придется ждать снаружи туалета,
      // пока кто-то из них не освободится.

      println("-----------")

      // Count Down Latch ( Защелка обратного отсчета  )

      // Защелка обратного отсчета — это примитив синхронизации, который ждет,
      // пока на нем не будет ожидающих заранее определенного количества волокон.
      // Защелка обратного отсчета инициализируется положительным числом.
      // Когда на ней вызывается освобождение, номер защелки уменьшается на 1.
      // Когда значение защелки становится равным 0, защелка открывается и выполнение возобновляется.
      // Это означает, что выполнение продолжится только в том случае,
      // если на защелке будет освобождено желаемое количество волокон.
      // Давайте реализуем сценарий множественного утверждения с защелкой обратного отсчета:

      def accessSafeLocker(approvals: CountDownLatch[IO]) = for {
        _ <- IO("Need to access safe locker.").trace
        _ <- approvals.await
        _ <- IO("Safe Locker opened and accessing the contents now").trace
      } yield ()

      def getApprovals(approvals: CountDownLatch[IO]) = for {
        _ <- IO("Requesting approvals for safe access").trace
        _ <- IO("Officer 1 Approval in progress").trace >> IO.sleep(Random.between(500, 1500).millis) >> IO("Officer 1 Approved").trace
        _ <- approvals.release
        _ <- IO("Officer 2 Approval in progress").trace >> IO.sleep(Random.between(500, 1500).millis) >> IO("Officer 2 Approved").trace
        _ <- approvals.release
        _ <- IO("Officer 3 Approval in progress").trace >> IO.sleep(Random.between(500, 1500).millis) >> IO("Officer 3 Approved").trace
        _ <- approvals.release
      } yield ()

      def safeAccessProcess: IO[Unit] = for {
        approvals <- CountDownLatch[IO](3)
        fib <- accessSafeLocker(approvals).start
        _ <- getApprovals(approvals)
        _ <- fib.join
      } yield ()

      safeAccessProcess.unsafeRunSync()

      // В вышеописанном процессе, только если 3 офицера одобряют запрос, то только тогда предоставляется доступ.
      // Если любой из офицеров не предоставляет доступ (открывает защелку), то доступ к сейфу невозможен.

      println("-----------")

      // Циклический барьер
      // Countdown Latch — это метод синхронизации одноразового использования.
      // После того, как защелка открыта, мы не можем повторно использовать CountDownLatch.
      // Cyclic Barrier почти похож на countdown latch, но его можно использовать повторно.

      // Мы можем создать циклический барьер, указав положительное число:

      val cyclicBarrier: IO[CyclicBarrier[IO]] = CyclicBarrier[IO](2)

      // Мы можем использовать метод await на cyclicBarrier для ожидания желаемого количества участников.
      // В приведенном выше коде, когда 2 волокна вызывают await на cyclicBarrier, он продолжает выполнение.

      // Заключение
      // В этой части мы рассмотрели различные примитивы для обработки синхронизации в нашем параллельном приложении.
      // Пример кода, используемый в этом блоге, доступен на GitHub в пакете part7.

      // https://github.com/yadavan88/cats-effect-intro
    }
  }
    part1.chapter1



}

