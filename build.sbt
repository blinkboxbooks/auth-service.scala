// This is needed due to a bug in the scala reflection that makes tests intermittently fail.
// See: https://issues.scala-lang.org/browse/SI-6240
val testSettings = Seq(
  parallelExecution in Test := true
)

val buildSettings = Seq(
  name := "auth-service-public",
  organization := "com.blinkbox.books.zuul",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion := "2.11.2",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7"),
  unmanagedResourceDirectories in Compile += file("./var")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val akkaV = "2.3.6"
    val sprayV = "1.3.1"
    Seq(
      "io.spray"                  %%  "spray-client"          % sprayV,
      "com.zaxxer"                %   "HikariCP-java6"        % "2.0.1",
      "commons-lang"              %   "commons-lang"          % "2.6",
      "com.lambdaworks"           %   "scrypt"                % "1.4.0",
      "com.maxmind.geoip2"        %   "geoip2"                % "0.9.0",
      "com.blinkbox.books"        %%  "common-slick"          % "0.1.1",
      "com.blinkbox.books"        %%  "common-spray"          % "0.16.2",
      "com.blinkbox.books"        %%  "common-spray-auth"     % "0.7.0",
      "com.blinkbox.books.hermes" %%  "rabbitmq-ha"           % "6.0.5",
      "com.blinkbox.books.hermes" %%  "message-schemas"       % "0.6.2",
      "com.blinkbox.books"        %%  "common-scala-test"     % "0.3.0"   % "test",
      "io.spray"                  %%  "spray-testkit"         % sprayV    % "test",
      "xmlunit"                   %   "xmlunit"               % "1.5"     % "test"
    )
  }
)

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*).
  settings(testSettings: _*)
