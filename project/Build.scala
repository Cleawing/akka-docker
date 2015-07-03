import org.stormenroute.mecha._
import sbt._
import sbt.Keys._

object DockerScalaBuild extends MechaRepoBuild {
  lazy val dockerScalaSettings = Defaults.defaultSettings ++
    MechaRepoPlugin.defaultSettings ++ Seq(
    name := "docker-scala",
    scalaVersion := "2.11.7",
    version := "0.1",
    organization := "com.cleawing",
    libraryDependencies ++= superRepoDependencies("docker-scala") ++ Dependencies.akka
  )

  def repoName = "docker-scala"

  lazy val dockerScala: Project = Project(
    "docker-scala",
    file("."),
    settings = dockerScalaSettings
  ) dependsOnSuperRepo
}
