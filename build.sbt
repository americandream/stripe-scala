name := "stripe-scala"

version := io.Source.fromFile("VERSION").mkString.trim

organization := "com.stripe"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "[4.1, 4.2)" % "provided",
  "org.json4s" %% "json4s-native" % "3.1.0",
  "play" %% "play" % "2.1.0" % "provided",
  "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"
)

scalacOptions ++= "-deprecation" :: "-unchecked" :: "-language:implicitConversions" :: Nil           

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>http://www.stripe.com/</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://www.opensource.org/licenses/mit-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:stripe/stripe-scala.git</url>
    <connection>scm:git:git@github.com:stripe/stripe-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>siddarth</id>
      <name>Siddarth Chandrasekaran</name>
      <url>https://stripe.com/about#siddarth</url>
    </developer>
    <developer>
       <id>jasong</id>
       <name>Jason Gilanfarr</name>
       <email>jason.gilanfarr@gmail.com</email>
    </developer>
  </developers>
)

