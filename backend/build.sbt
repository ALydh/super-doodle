scalaVersion := "3.3.4"

enablePlugins(NativeImagePlugin, JavaAppPackaging, AssemblyPlugin)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "co.fs2" %% "fs2-core" % "3.9.3",
  "co.fs2" %% "fs2-io" % "3.9.3",
  "com.comcast" %% "ip4s-core" % "3.2.0",
  "org.http4s" %% "http4s-ember-server" % "0.23.24",
  "org.http4s" %% "http4s-dsl" % "0.23.24",
  "org.http4s" %% "http4s-circe" % "0.23.24",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "org.jsoup" % "jsoup" % "1.17.2",
  "org.tpolecat"  %% "doobie-core" % "1.0.0-RC5",
  "org.xerial"     % "sqlite-jdbc" % "3.44.1.0",
  "org.mindrot"    % "jbcrypt" % "0.4",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

Compile / run / fork := true

// Native Image Configuration
Compile / mainClass := Some("wahapedia.Main")

nativeImageOptions := Seq(
  "--no-fallback",
  "--enable-url-protocols=http,https",
  "-H:+ReportExceptionStackTraces",
  "-H:+UnlockExperimentalVMOptions",
  "--gc=serial", // smaller footprint for low load
  "--initialize-at-build-time",
  "-H:IncludeResources=.*\\.csv$",
  "-H:IncludeResources=application\\.conf.*",
  "--allow-incomplete-classpath"
)

// Assembly configuration for fallback
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "application.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}
