import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.trueaccord.scalapb.{ScalaPbPlugin => PB}
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.SbtNativePackager

object MyBuild extends Build {

  override lazy val settings = (super.settings ++ Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-language:reflectiveCalls",
      "-Yno-adapted-args",
      "-Ywarn-numeric-widen",
      "-Xfuture",
      "-Xlint"
    )
  ))

  lazy val sharedSettings =
    // Protobuf will be executed in both js and server projects
    PB.protobufSettings ++
    Seq(
      // Add shared protobuf directory to config
      (sourceDirectory in PB.protobufConfig) :=
        file(root.base.getAbsolutePath) / "shared" / "src" / "main" / "protobuf",
      // Add source directory to compile stage (scalajs and scala/jvm)
      (unmanagedSourceDirectories in Compile) +=
        file(root.base.getAbsolutePath) / "shared" / "src"
    )

  lazy val js = Project(id = "js", base = file("js")).
    settings(sharedSettings).
    settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.0",
        "com.trueaccord.scalapb" %%% "scalapb-runtime" % "0.5.34",
        "com.trueaccord.scalapb" %%% "scalapb-runtime" % "0.5.34" % PB.protobufConfig
      )
    ).
    enablePlugins(ScalaJSPlugin)

  lazy val server = Project(id = "server", base = file("server")).
    settings(sharedSettings).
    settings(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.8",
        "com.typesafe.akka" %% "akka-remote" % "2.4.8",
        "com.typesafe.akka" %% "akka-contrib" % "2.4.8",
        "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8"
      )
    )

  lazy val root: Project = Project(id = "root", base = file(".")).
    aggregate(server, js)
}
