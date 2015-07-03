import sbt._

object Dependencies {
  object Versions {
    val akka = "2.4-M2"
  }

  lazy val akka = Seq(
    "com.typesafe.akka" %%  "akka-actor"    % Versions.akka,
    "com.typesafe.akka" %%  "akka-remote"   % Versions.akka,
    "com.typesafe.akka" %%  "akka-cluster"  % Versions.akka,
    "com.typesafe.akka" %%  "akka-testkit"  % Versions.akka % "test"
  )
}