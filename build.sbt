ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0"

val chiselVersion = "5.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "Musvit",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "5.0.0" % "test",
      "com.github.scopt" %% "scopt" % "4.1.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations",
    ),
    addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full),
  )

