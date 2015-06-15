sbtPlugin := true

organization := "com.github.germanosin.sbt"

name := "sbt-less4j"

version := "1.0.1"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.github.sommeri" % "less4j" % "1.12.0",
  "commons-logging" % "commons-logging" % "1.2"
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
  val nexus = "https://oss.sonatype.org"
  if (isSnapshot.value) Some("snapshots" at s"$nexus/content/repositories/snapshots")
  else Some("releases" at s"$nexus/service/local/staging/deploy/maven2")
}

pomExtra := (
      <url>http://github.com/germanosin/sbt-less4j</url>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>http://opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:germanosin/sbt-less4j.git</url>
        <connection>scm:git:git@github.com:germanosin/sbt-less4j.git</connection>
      </scm>
      <developers>
        <developer>
          <id>germanosin</id>
          <name>German Osin</name>
        </developer>
      </developers>
    )

useGpg := true
