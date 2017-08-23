package com.virtuslab

import akka.pattern.pipe
import akka.actor.Actor
import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer
import com.virtuslab.atlas.AtlasClient
import com.virtuslab.atlas.Model.{SparkApplication, SparkInputs, SparkOutputs}
import org.apache.spark.scheduler.{SparkListenerApplicationEnd, SparkListenerApplicationStart}

class MetadataAggregator(create: Boolean) extends Actor {

  import context.dispatcher
  private implicit val materializer = ActorMaterializer()

  private val atlasClient = new AtlasClient()(context.system)
  private var state = Option.empty[SparkApplication]

  override def receive: Receive = {
    case event: SparkListenerApplicationStart =>
      state = Some(SparkApplication(event))

    case SparkInputs(inputs) =>
      state = state.map(_.copy(inputs = inputs))

    case SparkOutputs(outputs) =>
      state = state.map(_.copy(outputs = outputs))

    case SparkListenerApplicationEnd(time) =>
      state = state.map(_.copy(endTime = time))
      atlasClient.createSparkApplication(state.get, create).pipeTo(self)

    case HttpResponse(code, headers, entity, _) =>
      println("Request response code: " + code)
      entity.discardBytes()
      context.system.terminate()

    case _ =>

  }
}