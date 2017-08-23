package com.virtuslab.atlas

import com.virtuslab.atlas.API.{AtlasAttribute, AtlasMetatype, AtlasMetatypeWrapper}
import org.apache.spark.scheduler.SparkListenerApplicationStart

object Model {

  val sparkApplication = AtlasMetatypeWrapper(Seq(AtlasMetatype(
          name = "spark_application",
          superTypes = Seq("Process"),
          typeVersion = "1.0",
          attributesDefs = Seq(
            AtlasAttribute("startTime","date"),
            AtlasAttribute("endTime","date")
          ))))
  trait Asset {
    def name: String

    def description: Option[String]

    def owner: Option[String]
  }

  trait Referenceable {
    def qualifiedName: String
  }

  trait DataSet extends Asset with Referenceable

  trait Process extends Asset with Referenceable {
    def inputs: List[DataSet]

    def outputs: List[DataSet]
  }

  case class FsPath(path: String,
                    qualifiedName: String,
                    name: String,
                    description: Option[String] = None,
                    owner: Option[String] = None) extends DataSet

  object FsPath {
    def apply(path: String): FsPath = FsPath(path, path, path)
  }

  case class SparkInputs(seq: List[DataSet])

  case class SparkOutputs(seq: List[DataSet])

  case class SparkApplication(qualifiedName: String,
                              name: String,
                              description: Option[String],
                              owner: Option[String],
                              startTime: Long,
                              endTime: Long,
                              inputs: List[DataSet],
                              outputs: List[DataSet]) extends Process

  object SparkApplication {
    def apply(event: SparkListenerApplicationStart): SparkApplication = {
      new SparkApplication(
        event.appId.getOrElse(event.appName),
        event.appName,
        None,
        Some(event.sparkUser),
        event.time,
        0,
        List.empty,
        List.empty
      )
    }
  }

}

