package kz.anon.portal.mainapi

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout

import kz.anon.portal.service.{AuthActor, User}
import kz.anon.portal.service.AuthActor.{ActionPerformed, CreateUser, GetUser}
import kz.anon.portal.serializer.Json4sSerializer

import scala.concurrent.Future

class MainApi(authActor: ActorRef[AuthActor.Command])(implicit val system: ActorSystem[_]) extends Json4sSerializer {

  implicit private val timeout: Timeout = Timeout.create(system.settings.config.getDuration("timeout"))

  def getUser(id: String): Future[ActionPerformed] =
    authActor.ask(GetUser(id, _))

  def createUser(user: User): Future[ActionPerformed] =
    authActor.ask(CreateUser(user, _))

  val mainRoutes: Route = {
    concat(
      pathPrefix("auth") {
        concat(
          get {
            parameter('id) { id =>
              complete(getUser(id))
            }
          },
          post {
            entity(as[User]) { user =>
              onSuccess(createUser(user)) { actionPerformed =>
                complete(StatusCodes.Created, actionPerformed)
              }
            }
          }
        )
      }
    )

  }

}
