inThisBuild(
  List(
    scalaVersion := "3.7.4",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
// scalafmt disabled
enablePlugins(NativeImagePlugin, JavaAppPackaging, AssemblyPlugin)

val tapirVersion = "1.11.11"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "org.typelevel" %% "cats-effect" % "3.6.1",
  "co.fs2" %% "fs2-core" % "3.12.2",
  "co.fs2" %% "fs2-io" % "3.12.2",
  "com.comcast" %% "ip4s-core" % "3.7.0",
  "org.http4s" %% "http4s-ember-server" % "0.23.30",
  "org.http4s" %% "http4s-ember-client" % "0.23.30",
  "org.http4s" %% "http4s-dsl" % "0.23.30",
  "org.http4s" %% "http4s-circe" % "0.23.30",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
  "io.circe" %% "circe-core" % "0.14.10",
  "io.circe" %% "circe-generic" % "0.14.10",
  "org.jsoup" % "jsoup" % "1.17.2",
  "org.tpolecat"  %% "doobie-core" % "1.0.0-RC12",
  "org.xerial"     % "sqlite-jdbc" % "3.51.3.0",
  "org.mindrot"    % "jbcrypt" % "0.4",
  "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
  "ch.linkyard.mcp" %% "mcp-server" % "0.3.3",
  "ch.linkyard.mcp" %% "mcp-server-http4s" % "0.3.3",
  "com.melvinlow" %% "scala-json-schema" % "0.2.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

scalacOptions ++= Seq("-Xmax-inlines", "64", "-Wunused:all")

Compile / run / fork := true

// Native Image Configuration
Compile / mainClass := Some("wp40k.Main")
nativeImageGraalHome := file(sys.env.getOrElse("GRAALVM_HOME", "/usr/lib/jvm/graalvm")).toPath

nativeImageOptions := Seq(
  "--no-fallback",
  "--enable-url-protocols=http,https",
  "-H:+ReportExceptionStackTraces",
  "-H:+UnlockExperimentalVMOptions",
  "--gc=serial", // smaller footprint for low load
  "--initialize-at-run-time=wp40k.db.DatabaseConfig$,wp40k.Main$",
  "-H:IncludeResources=.*\\.csv$",
  "-H:IncludeResources=application\\.conf.*",
  "-H:IncludeResources=META-INF/resources/webjars/swagger-ui/.*",
  "--allow-incomplete-classpath"
)

// Assembly configuration for fallback
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "application.conf"       => MergeStrategy.concat
  case x                        => MergeStrategy.first
}
