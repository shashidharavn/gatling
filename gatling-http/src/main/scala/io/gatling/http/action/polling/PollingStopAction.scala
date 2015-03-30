package io.gatling.http.action.polling

import akka.actor.{ ActorRef, Props }
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session._
import io.gatling.http.action.UnnamedRequestAction

object PollingStopAction {
  def props(pollingName: String, dataWriters: DataWriters, next: ActorRef): Props =
    Props(new PollingStopAction(pollingName, dataWriters, next))
}
class PollingStopAction(
  pollingName: String,
  dataWriters: DataWriters,
  val next: ActorRef)
    extends UnnamedRequestAction(dataWriters) {

  override def sendRequest(requestName: String, session: Session) = ???
}
