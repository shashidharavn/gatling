package io.gatling.http.action.polling

import scala.concurrent.duration.Duration

import akka.actor.{ ActorSystem, ActorRef }
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.config.DefaultHttpProtocol
import io.gatling.http.request.builder.HttpRequestBuilder

class PollingStartBuilder(
  pollingName: String,
  duration: Expression[Duration],
  requestBuilder: HttpRequestBuilder)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(PollingStartAction.props(pollingName, duration, requestBuilder, ctx.dataWriters, next), actorName("pollingStart"))
}

class PollingStopBuilder(
  pollingName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(PollingStopAction.props(pollingName, ctx.dataWriters, next), actorName("pollingStop"))
}