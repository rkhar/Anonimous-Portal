package kz.anon.portal.signup.login.api.service

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.{Directive1, Route, RouteResult}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.anon.portal.signup.login.api.model.CommonEntity

import scala.concurrent.Promise

trait ParentService extends Json4sSupport{

  implicit def system: ActorSystem

  def createSomeActor(targetProps: Props, message: CommonEntity): Route = ctx => {
    val p = Promise[RouteResult]
    system.actorOf(targetProps)
    p.future
  }

}
