name := "tadx"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-optimize",
                      "-deprecation",
                      "-encoding", "utf8",
                      "-feature",
                      "-unchecked")

retrieveManaged := true

resolvers += "Spray Repository" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.1.0",
                            "com.typesafe.akka" %% "akka-actor" % "2.1.0",
                            "com.typesafe.akka" %% "akka-agent" % "2.1.0",
                            "io.spray" % "spray-can" % "1.1-M7",
                            "io.spray" % "spray-routing" % "1.1-M7",
                            "io.spray" %% "spray-json" % "1.2.3",
                            "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test",
                            "org.specs2" %% "specs2" % "1.13" % "test",
                            "junit" % "junit" % "4.5" % "test")

seq(Revolver.settings: _*)
