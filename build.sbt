name := "training-gnutella"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += Opts.resolver.sonatypeSnapshots

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "commons-io" % "commons-io" % "2.4",
  "commons-codec" % "commons-codec" % "1.10",
  "org.scalafx" %% "scalafx" % "8.0.40-R9-SNAPSHOT",
  "org.scala-lang" % "scala-reflect" % "2.11.6"
)

unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar"))
