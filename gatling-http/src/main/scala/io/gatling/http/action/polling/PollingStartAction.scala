package io.gatling.http.action.polling

import akka.actor.{ ActorRef, Props }
import io.gatling.core.action.Interruptable
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration.Duration

object PollingStartAction {
  def props(pollingName: String,
            duration: Expression[Duration],
            requestBuilder: HttpRequestBuilder,
            dataWriters: DataWriters,
            next: ActorRef): Props =
    Props(new PollingStartAction(pollingName, duration, requestBuilder, dataWriters, next))
}
class PollingStartAction(
    pollingName: String,
    duration: Expression[Duration],
    requestBuilder: HttpRequestBuilder,
    val dataWriters: DataWriters,
    val next: ActorRef) extends Interruptable {

  override def execute(session: Session): Unit = ???
}
