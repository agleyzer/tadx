tadx
====

Toy ad server in Akka/Scala.

To run, in sbt:

    ~ re-start --- -server

To test:

    ab -c 50 -n 100000 -k http://localhost:8080/tadx/ads\?positions\=top,bottom,left,right,foo,bar,bang

To get stats:

    curl  http://localhost:8080/tadx/stats
