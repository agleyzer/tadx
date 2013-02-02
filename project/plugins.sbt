addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.0.10")


addSbtPlugin("io.spray" % "sbt-revolver" % "0.6.2")


resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.3.0-SNAPSHOT")
