import scalapb.compiler.Version._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val jsoniterVersion = settingKey[String]("")
val scalapbJsonCommonVersion = settingKey[String]("")

val scalapbJsoniter = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    name := "scalapb-jsoniter",
    libraryDependencies += "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % jsoniterVersion.value,
    buildInfoPackage := "scalapb_jsoniter",
    buildInfoObject := "ScalapbJsoniterBuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      "scalapbVersion" -> scalapbVersion,
      jsoniterVersion,
      scalapbJsonCommonVersion,
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
      "com.google.protobuf" % "protobuf-java-util" % "3.25.8" % "test",
      "com.google.protobuf" % "protobuf-java" % "3.25.8" % "protobuf"
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    (Test / PB.targets) ++= Seq[protocbridge.Target](
      scalapb.gen(javaConversions = false, scala3Sources = true) -> (Test / sourceManaged).value
    )
  )

lazy val macros = project
  .in(file("macros"))
  .settings(
    commonSettings,
    name := "scalapb-jsoniter-macros",
    Test / PB.protoSources := Nil,
    Test / PB.targets := Nil,
    libraryDependencies ++= Seq(
      "io.github.scalapb-json" %% "scalapb-json-macros" % scalapbJsonCommonVersion.value,
    ),
  )
  .dependsOn(
    scalapbJsoniterJVM
  )

lazy val tests = project
  .in(file("tests"))
  .settings(
    commonSettings,
    noPublish,
    Test / PB.protoSources := Nil,
    Test / PB.targets := Nil,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest-funspec" % "3.2.19" % "test",
    ),
  )
  .dependsOn(macros, scalapbJsoniterJVM % "test->test")

commonSettings

val noPublish = Seq(
  publishLocal := {},
  publish := {},
  Compile / publishArtifact := false
)

noPublish

Global / excludeLintKeys += jsoniterVersion

lazy val commonSettings = Def.settings(
  scalaVersion := "3.3.7",
  scalacOptions ++= Seq("-feature", "-deprecation", "-old-syntax", "-no-indent"),
  description := "Json/Protobuf convertors for ScalaPB using jsoniter-scala",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  Compile / PB.targets := Nil,
  (Test / PB.protoSources) := Seq(baseDirectory.value.getParentFile / "shared/src/test/protobuf"),
  scalapbJsonCommonVersion := "0.10.0",
  jsoniterVersion := "2.32.0",
  libraryDependencies ++= Seq(
    "io.github.scalapb-json" %%% "scalapb-json-common" % scalapbJsonCommonVersion.value,
    "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion % "protobuf,test",
    "org.scalatest" %%% "scalatest-flatspec" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-freespec" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-mustmatchers" % "3.2.19" % "test",
    "org.scalatest" %%% "scalatest-shouldmatchers" % "3.2.19" % "test",
    "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % jsoniterVersion.value % "test",
  ),
  compileOrder := CompileOrder.JavaThenScala,
)

val scalapbJsoniterJVM = scalapbJsoniter.jvm
val scalapbJsoniterJS = scalapbJsoniter.js
val scalapbJsoniterNative = scalapbJsoniter.native
