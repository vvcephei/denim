name := "denim"

organization := "org.vvcephei"

version := "0.3"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
    // jersey
    "org.apache.httpcomponents" % "httpclient" % "4.2.1",
    "com.sun.jersey" % "jersey-core" % "1.17.1",
    "com.sun.jersey" % "jersey-client" % "1.17.1",
    "com.sun.jersey" % "jersey-json" % "1.17.1",
    // jackson
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.2.3",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.2.3",
    "org.joda" % "joda-convert" % "1.2",
    "joda-time" % "joda-time" % "2.3",
    // jackson-yaml
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.1.3",
    // cli
    "com.beust" % "jcommander" % "1.30"
)

libraryDependencies ++= Seq(
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "org.mockito" % "mockito-core" % "1.9.0" % "test"
)

net.virtualvoid.sbt.graph.Plugin.graphSettings

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/vvcephei/denim</url>
  <licenses>
    <license>
      <name>Apache</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:vvcephei/denim.git</url>
    <connection>scm:git:git@github.com:vvcephei/denim.git</connection>
  </scm>
  <developers>
    <developer>
      <id>vvcephei</id>
      <name>John Roesler</name>
      <url>http://www.vvcephei.org</url>
    </developer>
  </developers>)


