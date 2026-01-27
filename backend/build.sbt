scalaVersion := "3.3.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "co.fs2" %% "fs2-core" % "3.9.3",
  "co.fs2" %% "fs2-io" % "3.9.3",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "org.jsoup" % "jsoup" % "1.17.2",
  "org.tpolecat"  %% "doobie-core" % "1.0.0-RC5",
  "org.xerial"     % "sqlite-jdbc" % "3.44.1.0",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)