import scalapb.compiler.Version._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val jsoniterVersion = settingKey[String]("")

val scalapbJsoniter = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "scalapb-jsoniter",
    libraryDependencies += "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterVersion.value,
    buildInfoPackage := "scalapb_jsoniter",
    buildInfoObject := "ScalapbJsoniterBuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      "scalapbVersion" -> scalapbVersion,
      jsoniterVersion,
      scalaVersion,
      version
    )
  )
  .jvmSettings(
    (Test / PB.targets) ++= Seq[protocbridge.Target](
      PB.gens.java -> (Test / sourceManaged).value,
      scalapb.gen(javaConversions = true, scala3Sources = true) -> (Test / sourceManaged).value
    ),
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java-util" % "4.35.0" % "test",
      "com.google.protobuf" % "protobuf-java" % "4.35.0" % "protobuf"
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    (Test / PB.targets) ++= Seq[protocbridge.Target](
      scalapb.gen(javaConversions = false, scala3Sources = true) -> (Test / sourceManaged).value
    )
  )
  .nativeSettings(
    // scalatest-core 3.2.19 for Native is built against test-interface 0.5.1, which declares a strict scheme
    libraryDependencySchemes += "org.scala-native" % "test-interface_native0.5_3" % VersionScheme.Always
  )

lazy val macros = project
  .in(file("macros"))
  .settings(
    name := "scalapb-jsoniter-macros",
    Test / PB.protoSources := Nil,
    Test / PB.targets := Nil,
  )
  .dependsOn(
    scalapbJsoniterJVM
  )

lazy val tests = project
  .in(file("tests"))
  .settings(
    noPublish,
    Test / PB.protoSources := Nil,
    Test / PB.targets := Nil,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest-funspec" % "3.2.19" % "test",
      "io.github.scalapb-json" %% "scalapb-circe" % "0.16.0" % "test",
      "io.circe" %% "circe-parser" % "0.14.15" % "test",
    ),
    // scalapb-circe 0.16.0 is built on scalapb-runtime 0.11.x; the comparison tests run it against 1.0.0-alpha
    libraryDependencySchemes += "com.thesamet.scalapb" %% "scalapb-runtime" % VersionScheme.Always,
  )
  .dependsOn(macros, scalapbJsoniterJVM % "test->test")

commonSettings

val noPublish = Seq(
  publish / skip := true,
)

Global / excludeLintKeys += jsoniterVersion

lazy val commonSettings = Def.settings(
  scalaVersion := "3.3.7",
  scalacOptions ++= Seq("-feature", "-deprecation", "-old-syntax", "-no-indent", "-Werror"),
  description := "Json/Protobuf convertors for ScalaPB using jsoniter-scala",
  licenses += ("MIT", uri("https://opensource.org/licenses/MIT")),
  organization := "io.github.hoangmaihuy",
  versionScheme := Some("early-semver"),
  homepage := Some(uri("https://github.com/hoangmaihuy/scalapb-jsoniter")),
  scmInfo := Some(
    ScmInfo(
      uri("https://github.com/hoangmaihuy/scalapb-jsoniter"),
      "scm:git:github.com/hoangmaihuy/scalapb-jsoniter.git",
      Some("scm:git:git@github.com:hoangmaihuy/scalapb-jsoniter.git")
    )
  ),
  developers := List(
    Developer("hoangmaihuy", "Hoang Mai", "", uri("https://github.com/hoangmaihuy"))
  ),
  Compile / PB.targets := Nil,
  (Test / PB.protoSources) := Seq(baseDirectory.value.getParentFile / "shared/src/test/protobuf"),
  jsoniterVersion := "2.32.0",
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
    "org.scalatest" %% "scalatest-flatspec" % "3.2.19" % "test",
    "org.scalatest" %% "scalatest-freespec" % "3.2.19" % "test",
    "org.scalatest" %% "scalatest-mustmatchers" % "3.2.19" % "test",
    "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.19" % "test",
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterVersion.value % "test",
  ),
  compileOrder := CompileOrder.JavaThenScala,
)

val scalapbJsoniterJVM = scalapbJsoniter.jvm
val scalapbJsoniterJS = scalapbJsoniter.js
val scalapbJsoniterNative = scalapbJsoniter.native
