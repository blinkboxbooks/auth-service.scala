val buildSettings = Seq(
  name := "auth-service-public",
  organization := "com.blinkbox.books.zuul",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion := "2.11.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7"),
  unmanagedResourceDirectories in Compile += file("./var")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val sprayV = "1.3.2"
    Seq(
      "io.spray"                  %%  "spray-client"          % sprayV,
      "com.zaxxer"                %   "HikariCP-java6"        % "2.1.0",
      "commons-lang"              %   "commons-lang"          % "2.6",
      "com.lambdaworks"           %   "scrypt"                % "1.4.0",
      "com.maxmind.geoip2"        %   "geoip2"                % "0.9.0",
      "com.blinkbox.books"        %%  "common-config"         % "1.4.1",
      "com.blinkbox.books"        %%  "common-slick"          % "0.2.0",
      "com.blinkbox.books"        %%  "common-spray"          % "0.17.3",
      "com.blinkbox.books"        %%  "common-spray-auth"     % "0.7.2",
      "com.blinkbox.books.hermes" %%  "rabbitmq-ha"           % "7.1.0",
      "com.blinkbox.books.hermes" %%  "message-schemas"       % "0.7.2",
      "com.blinkbox.books"        %%  "common-scala-test"     % "0.3.0"   % Test,
      "io.spray"                  %%  "spray-testkit"         % sprayV    % Test,
      "xmlunit"                   %   "xmlunit"               % "1.5"     % Test
    )
  }
)

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*)
