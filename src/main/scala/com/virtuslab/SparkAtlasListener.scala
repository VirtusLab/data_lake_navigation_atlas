package com.virtuslab

import akka.actor.ActorRef
import org.apache.spark.SparkFirehoseListener
import org.apache.spark.scheduler._

class SparkAtlasListener(actor: ActorRef) extends SparkFirehoseListener {

  override def onEvent(event: SparkListenerEvent): Unit = {
    actor ! event
  }

}
