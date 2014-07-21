
val buildSettings = Seq(
  name := "auth-server",
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
      "io.spray"              %   "spray-can"             % sprayV,
      "io.spray"              %   "spray-routing"         % sprayV,
      "io.spray"              %%  "spray-json"            % "1.2.6",
      "io.spray"              %   "spray-client"          % sprayV,
      "org.json4s"            %%  "json4s-jackson"        % json4sV,
      "org.json4s"            %%  "json4s-native"         % json4sV, // for swagger :-/
      "org.json4s"            %%  "json4s-ext"            % json4sV,
      "com.typesafe.akka"     %%  "akka-actor"            % akkaV,
      "com.typesafe.akka"     %%  "akka-slf4j"            % akkaV,
      "com.typesafe.slick"    %% "slick"                  % "2.0.2",
      "com.gettyimages"       %%  "spray-swagger"         % "0.4.3",
      "commons-lang"          %   "commons-lang"          % "2.6",
      "ch.qos.logback"        %   "logback-classic"       % "1.1.2",
      "com.lambdaworks"       %   "scrypt"                % "1.4.0",
      "com.blinkbox.books"    %%  "common-config"         % "0.7.0",
      "com.blinkbox.books"    %%  "common-spray"          % "0.12.0",
      "com.blinkbox.books"    %%  "common-spray-auth"     % "0.5.0",
      "mysql"                 %   "mysql-connector-java"  % "5.1.31",
      "io.spray"              %   "spray-testkit"         % sprayV    % "test",
      "com.typesafe.akka"     %%  "akka-testkit"          % akkaV     % "test",
      "org.scalatest"         %%  "scalatest"             % "2.2.0"   % "test",
      "junit"                 %   "junit"                 % "4.11"    % "test",
      "com.novocode"          %   "junit-interface"       % "0.10"    % "test",
      "org.mockito"           %   "mockito-all"           % "1.9.5"   % "test",
      "xmlunit"               %   "xmlunit"               % "1.5"     % "test"
    )
  }
)

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*)
