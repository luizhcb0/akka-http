
name := "akka-http"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.5"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.12" // or whatever the latest version is

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5"

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

//assemblyMergeStrategy in assembly := {
//  case m if m.toLowerCase.contains("example") => MergeStrategy.discard
//  case PathList("service.conf") => MergeStrategy.discard
//  case PathList("system.conf") => MergeStrategy.discard
//  case PathList("br.com.bb.horus.example")  => MergeStrategy.discard
//  case PathList("logback.xml") => MergeStrategy.discard
//  case PathList("application.conf") => MergeStrategy.discard
//  case x =>
//    val oldStrategy = (assemblyMergeStrategy in assembly).value
//    oldStrategy(x)
//}

enablePlugins(JettyPlugin)
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"
//libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.0"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.1"