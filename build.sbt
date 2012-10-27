name := "tadx"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-optimize",
                      "-deprecation",
                      "-encoding", "utf8",
                      "-unchecked")

retrieveManaged := true

libraryDependencies ++= Seq("com.typesafe.akka" % "akka-actor" % "2.0.2",
                            "com.typesafe.akka" % "akka-actor" % "2.0.2",
                            "com.typesafe.akka" % "akka-agent" % "2.0.2",
                            "org.scala-tools" %% "scala-stm" % "0.5",
                            "cc.spray" % "spray-can" % "1.0-M2",
                            "cc.spray" % "spray-server" % "1.0-M2",
                            "cc.spray" %% "spray-json" % "1.1.1",
                            "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test",
                            "org.specs2" %% "specs2" % "1.9" % "test",
                            "junit" % "junit" % "4.5" % "test")

seq(Revolver.settings: _*)
