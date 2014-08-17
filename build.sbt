sbtPlugin := true

organization := "com.github.germanosin.sbt"

name := "sbt-less4j"

version := "1.0-SNAPSHOT1"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
	"com.github.sommeri" % "less4j" % "1.8.0"
)

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)


addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.0.2")

publishMavenStyle := true

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }