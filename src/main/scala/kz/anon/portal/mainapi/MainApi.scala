package kz.anon.portal.mainapi

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import kz.anon.portal.service.{AuthActor, Users}
import kz.anon.portal.service.AuthActor.GetUsers
import kz.anon.portal.serializer.Json4sSerializer

import scala.concurrent.Future

class MainApi(authActor: ActorRef[AuthActor.Command])(implicit val system: ActorSystem[_]) extends Json4sSerializer {

  implicit private val timeout: Timeout = Timeout.create(system.settings.config.getDuration("timeout"))

  def getUsers: Future[Users] =
    authActor.ask(GetUsers)

  val mainRoutes: Route = {
    concat(
      pathPrefix("auth") {
        concat(
          pathEnd {
            concat(
              get {
                complete(getUsers)
              }
            )
          }
        )
      }
    )

  }

}
