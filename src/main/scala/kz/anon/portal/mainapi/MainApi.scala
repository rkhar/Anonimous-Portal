package kz.anon.portal.mainapi

import java.io.InputStream

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{Directive1, Route}
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
  DocumentReceived,
  DocumentToReceive,
  GetDocument,
  GetUser,
  Login,
  PostDocument,
  TokenResponse,
  UpdateUser,
  UpdateUserModel,
  User,
  UserReceived
}
import kz.anon.portal.serializer.Json4sSerializer

import scala.concurrent.Future

class MainApi(mainActor: ActorRef[MainActor.Command])(implicit val system: ActorSystem[_])
    extends Json4sSerializer
    with PredefinedFromStringUnmarshallers {

  implicit private val timeout: Timeout = Timeout.create(system.settings.config.getDuration("timeout"))

  def getUser(headers: Map[String, String], id: String): Future[UserReceived] = mainActor.ask(GetUser(headers, id, _))
  def createUser(user: User): Future[TokenResponse]                           = mainActor.ask(CreateUser(user, _))

  def updateUser(headers: Map[String, String], id: String, password: String): Future[ActionPerformed] =
    mainActor.ask(UpdateUser(headers, id, password, _))

  def deleteUser(headers: Map[String, String], id: String): Future[ActionPerformed] =
    mainActor.ask(DeleteUser(headers, id, _))

  def login(privateName: String, password: String): Future[TokenResponse] =
    mainActor.ask(Login(privateName, password, _))

  def getDocument(id: String): Future[DocumentReceived] = mainActor.ask(GetDocument(id, _))

  def postDocument(
      userId: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[String]]
  ): Future[ActionPerformed] = mainActor.ask(PostDocument(userId, latLng, center, zoom, message, categories, files, _))
//  def updateDocument(id: String, is: InputStream): Future[ActionPerformed] = mainActor.ask(PostDocument(id, is, _))
  def deleteDocument(id: String): Future[ActionPerformed] = mainActor.ask(DeleteDocument(id, _))

  val mainRoutes: Route = {
    concat(
      pathPrefix("auth") {
        concat(
          httpHeaders { headers =>
            parameter("id") { id =>
              onSuccess(getUser(headers, id)) { userReceived =>
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
          httpHeaders { headers =>
            put {
              entity(as[UpdateUserModel]) { updateUserModel =>
                onSuccess(updateUser(headers, updateUserModel.privateName, updateUserModel.password)) {
                  actionPerformed =>
                    complete(StatusCodes.Accepted, actionPerformed)
                }
              }
            }
          },
          httpHeaders { headers =>
            delete {
              parameter("id") { id =>
                onSuccess(deleteUser(headers, id)) { actionPerformed =>
                  complete(StatusCodes.Accepted, actionPerformed)
                }
              }
            }
          }
        )
      },
      pathPrefix("login") {
        post {
          entity(as[User]) { user =>
            onSuccess(login(user.privateName, user.password)) { actionPerformed =>
              complete(StatusCodes.OK, actionPerformed)
            }
          }
        }
      },
      pathPrefix("document") {
        concat(
          get {
            parameter("id") { id =>
              onSuccess(getDocument(id)) { documentReceived =>
                complete(StatusCodes.Accepted, documentReceived)
              }
            }
          },
//          post {
//            parameter("id") { id =>
//              fileUpload("filename") {
//                case (_, filestream) =>
//                  val inputStream = filestream.runWith(StreamConverters.asInputStream())
//                  onSuccess(postDocument(id, inputStream)) { actionPerformed =>
//                    complete(StatusCodes.Created, actionPerformed)
//                  }
//              }
//            }
//          },
          post {
            entity(as[DocumentToReceive]) { doc =>
              onSuccess(
                postDocument(doc.userId, doc.latLng, doc.center, doc.zoom, doc.message, doc.categories, doc.files)
              ) { actionPerformed =>
                complete(StatusCodes.Created, actionPerformed)
              }

            }
          },
//          put {
//            parameter("id") { id =>
//              fileUpload("filename") {
//                case (_, filestream) =>
//                  val inputStream = filestream.runWith(StreamConverters.asInputStream())
//                  onSuccess(updateDocument(id, inputStream)) { actionPerformed =>
//                    complete(StatusCodes.Accepted, actionPerformed)
//                  }
//              }
//            }
//          },
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

  private def getHeaders(headers: Seq[HttpHeader]): Map[String, String] =
    headers.map { x =>
      (x.name, x.value)
    }.toMap

  private def httpHeaders: Directive1[Map[String, String]] = extract(c => getHeaders(c.request.headers))
}
