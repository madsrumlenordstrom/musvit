ThisBuild / scalaVersion     := "2.13.10"

val chiselVersion = "3.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "Musvit",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "com.github.scopt" %% "scopt" % "4.1.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  )