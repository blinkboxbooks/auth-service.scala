// This is needed due to a bug in the scala reflection that makes tests intermittently fail.
// See: https://issues.scala-lang.org/browse/SI-6240
val testSettings = Seq(
  parallelExecution in Test := false
)

val buildSettings = Seq(
  name := "auth-service-public",
  organization := "com.blinkbox.books.zuul",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion  := "2.10.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val akkaV = "2.3.4"
    val sprayV = "1.3.1"
    val json4sV = "3.2.10"
    Seq(
      "io.spray"                  %   "spray-client"          % sprayV,
      "org.json4s"                %%  "json4s-native"         % json4sV, // for swagger :-/
      "com.typesafe.akka"         %%  "akka-slf4j"            % akkaV,
      "com.typesafe.slick"        %%  "slick"                 % "2.1.0",
      "commons-lang"              %   "commons-lang"          % "2.6",
      "com.lambdaworks"           %   "scrypt"                % "1.4.0",
      "com.blinkbox.books"        %%  "common-config"         % "1.0.0",
      "com.blinkbox.books"        %%  "common-spray"          % "0.15.0",
      "com.blinkbox.books"        %%  "common-spray-auth"     % "0.5.0",
      "com.blinkbox.books"        %%  "common-json"           % "0.2.0",
      "com.blinkbox.books.hermes" %%  "rabbitmq-ha"           % "5.0.0",
      "com.blinkbox.books.hermes" %%  "message-schemas"       % "0.5.0",
      "mysql"                     %   "mysql-connector-java"  % "5.1.31",
      "com.blinkbox.books"        %%  "common-scala-test"     % "0.2.0"   % "test",
      "io.spray"                  %   "spray-testkit"         % sprayV    % "test",
      "xmlunit"                   %   "xmlunit"               % "1.5"     % "test",
      "com.h2database"            %   "h2"                    % "1.4.181" % "test"
    )
  }
)

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*).
  settings(testSettings: _*)
