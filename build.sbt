name := "cb-translation-memory"

version := "1.0"

scalaVersion := "2.11.5"

val sparkVersion = "1.2.1"

resolvers += "Typesafe Maven Repository" at "http://repo.typesafe.com/typesafe/maven-releases/"

resolvers += "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/"

resolvers += "Couchbase Repository" at "http://files.couchbase.com/maven2/"


libraryDependencies <<= scalaVersion {
  scalaVersion => Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
    // Spark and Mllib
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    // Lucene
    "org.apache.lucene" % "lucene-core" % "4.8.1",
    // for Porter Stemmer
    "org.apache.lucene" % "lucene-analyzers-common" % "4.8.1",
    // Guava for the dictionary
    "com.google.guava" % "guava" % "17.0",
    // Couchbase
    "org.reactivecouchbase" %% "reactivecouchbase-core" % "0.3",
    // Couchbase Spark Connector
    "com.couchbase.client" %% "spark-connector" % "1.0.0-dp"
  )
}
    