package com.virtuslab

import akka.actor.{ActorSystem, Props}
import com.virtuslab.atlas.Model.{FsPath, SparkInputs, SparkOutputs}
import org.apache.spark.{SparkConf, SparkContext}

object WordCount {

  def main(args: Array[String]): Unit = {
    val create = args.toList.find(_ == "--create").isDefined
    val inputPath = "input/rfc7540.txt"
    val outputPath = "target/output/rfc7540-wc"
    val appName = "WordCount"

    val actorSystem = ActorSystem(appName)
    val metadataAggregator = actorSystem.actorOf(Props(classOf[MetadataAggregator], create), "MetadataAggregator")

    val conf = new SparkConf().setAppName(appName).setMaster("local[*]")
    val sc = new SparkContext(conf)
    sc.addSparkListener(new SparkAtlasListener(metadataAggregator))

    try {
      sc.textFile(inputPath)
        .map(_.toLowerCase).setName("converted lines to lower case")
        .flatMap(text => text.split("""\W+""")).setName("splited in to words")
        .map(word => (word, 1)).setName("grouped by word")
        .reduceByKey(_ + _).setName("counted words")
        .saveAsTextFile(outputPath)

      metadataAggregator ! SparkInputs(List(FsPath(inputPath)))
      metadataAggregator ! SparkOutputs(List(FsPath(outputPath)))
    } finally {
//      Console.in.read() //localhost:4040
      sc.stop()
    }
  }
}
