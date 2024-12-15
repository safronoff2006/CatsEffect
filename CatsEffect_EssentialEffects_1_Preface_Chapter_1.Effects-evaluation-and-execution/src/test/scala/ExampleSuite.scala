import cats.effect.{IO, SyncIO}
import munit.CatsEffectSuite

class ExampleSuite extends CatsEffectSuite {
  test("make sure IO computes the right result") {
    IO.pure(1).map(_ + 2) flatMap { result =>
      IO(assertEquals(result, 3))
    }
  }
}

import weaver._

object ExampleSuiteWeaver  extends SimpleIOSuite {
  test("make sure IO computes the right result") {
    IO.pure(1).map(_ + 2) map { result =>
      expect.eql(result, 3)
    }
  }
}

import cats.effect.testing.specs2.CatsEffect

import org.specs2.mutable.Specification

class ExampleSpec extends Specification with CatsEffect {
  "my example" should {
    "make sure IO computes the right result" in {
      IO.pure(1).map(_ + 2) flatMap { result =>
        IO(result mustEqual 3)
      }
    }
  }
}