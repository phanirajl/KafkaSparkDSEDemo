val globalSettings = Seq(
  version := "0.1",
  scalaVersion := "2.10.5",
  resolvers += ("DataStax Repo" at "https://datastax.artifactoryonline.com/datastax/public-repos/")
)
val akkaVersion = "2.3.12"
val sparkVersion = "1.6.1"

val kafkaVersion = "0.10.1.0"

lazy val producer = (project in file("producer"))
  .settings(name := "producer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= producerDeps)

lazy val consumer = (project in file("consumer"))
  .settings(name := "consumer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= consumerDeps)

lazy val producerDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.apache.kafka" % "kafka_2.10" % kafkaVersion
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

lazy val consumerDeps = Seq(
  "com.datastax.dse" % "dse-spark-dependencies" % "5.0.4" % "provided",
  "org.apache.spark"  %% "spark-streaming-kafka" % sparkVersion % "provided"
)
    