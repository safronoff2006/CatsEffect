import sbt.Keys.organization
// The simplest possible sbt build file is just one line:

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(

      scalaVersion := "2.13.14",
      organization := "safronoff2006",
      version := "1.0",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
        "org.typelevel" %% "cats-core" % "2.12.0",
        "org.typelevel" %% "cats-kernel" % "2.12.0",
        "org.typelevel" %% "cats-laws" % "2.11.0",
        "org.typelevel" %% "cats-free" % "2.12.0",
        "org.typelevel" %% "cats-testkit" % "2.12.0",
        "org.typelevel" %% "cats-effect-cps" % "0.4.0",
        "org.typelevel" %% "cats-effect" % "3.5.5",
        "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test,
        "com.disneystreaming" %% "weaver-cats" % "0.7.6" % Test,
        "org.typelevel" %% "cats-effect-testing-specs2" % "1.2.0" % Test,


),
name := "Cats Effect - Essential Effects"

) ),
scalacOptions ++= Seq(
  "-Xasync",
  "-Xfatal-warnings",
  "-Wnonunit-statement",
  "-Wconf:msg=unused value of type org.scalatest.Assertion:s",
  "-Wconf:msg=unused value of type org.specs2.specification.core.Fragment:s",
  "-Wconf:msg=unused value of type org.specs2.matcher.MatchResult:s",
  "-Wconf:msg=unused value of type org.scalamock:s",
  "-deprecation"


)
,
testFrameworks += new TestFramework("weaver.framework.CatsEffect")

)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")



// lazy val root = (project in file(".")).
//   settings(
//     inThisBuild(List(
//       organization := "ch.epfl.scala",
//       scalaVersion := "2.13.12"
//     )),
//     name := "hello-world"
//   )

// To learn more about multi-project builds, head over to the official sbt
// documentation at http://www.scala-sbt.org/documentation.html
