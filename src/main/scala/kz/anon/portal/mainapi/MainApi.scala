package kz.anon.portal.mainapi

import java.io.InputStream

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import akka.stream.scaladsl.StreamConverters
import akka.util.Timeout
import kz.anon.portal.service.MainActor
import kz.anon.portal.service.MainActor.{
  ActionPerformed,
  CreateUser,
  DeleteDocument,
  DeleteUser,
  GetUser,
  PostDocument,
  User,
  UserReceived
}
import kz.anon.portal.serializer.Json4sSerializer

import scala.concurrent.Future

class MainApi(mainActor: ActorRef[MainActor.Command])(implicit val system: ActorSystem[_])
    extends Json4sSerializer
    with PredefinedFromStringUnmarshallers {

  implicit private val timeout: Timeout = Timeout.create(system.settings.config.getDuration("timeout"))

  def getUser(id: String): Future[UserReceived] =
    mainActor.ask(GetUser(id, _))

  def createUser(user: User): Future[ActionPerformed] =
    mainActor.ask(CreateUser(user, _))

  def deleteUser(id: String): Future[ActionPerformed] =
    mainActor.ask(DeleteUser(id, _))

  def postDocument(id: String, inputStream: InputStream): Future[ActionPerformed] =
    mainActor.ask(PostDocument(id, inputStream, _))

  def deleteDocument(id: String): Future[ActionPerformed] =
    mainActor.ask(DeleteDocument(id, _))

  val mainRoutes: Route = {
    concat(
      pathPrefix("auth") {
        concat(
          get {
            parameter("id") { id =>
              onSuccess(getUser(id)) { userReceived =>
                complete(userReceived)
              }
            }
          },
          post {
            entity(as[User]) { user =>
              onSuccess(createUser(user)) { actionPerformed =>
                complete(StatusCodes.Created, actionPerformed)
              }
            }
          },
          delete {
            parameter("id") { id =>
              onSuccess(deleteUser(id)) { actionPerformed =>
                complete(StatusCodes.Accepted, actionPerformed)
              }
            }
          }
        )
      },
      pathPrefix("document") {
        concat(
          post {
            parameter("id") { id =>
              fileUpload("filename") {
                case (_, filestream) =>
                  val inputStream = filestream.runWith(StreamConverters.asInputStream())
                  onSuccess(postDocument(id, inputStream)) { actionPerformed =>
                    complete(StatusCodes.Accepted, actionPerformed)
                  }
              }
            }
          },
          delete {
            parameter("id") { id =>
              onSuccess(deleteDocument(id)) { actionPerformed =>
                complete(StatusCodes.Accepted, actionPerformed)
              }
            }
          }
        )
      }
    )

  }

}
