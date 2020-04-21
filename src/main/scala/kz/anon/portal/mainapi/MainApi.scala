package kz.anon.portal.mainapi

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import akka.util.Timeout
import kz.anon.portal.service.MainActor
import kz.anon.portal.service.MainActor._
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

  def getAllDocuments(start: Int, limit: Int): Future[DocumentsReceived] =
    mainActor.ask(GetAllDocuments(start, limit, _))

  def getUsersDocuments(id: String, start: Int, limit: Int): Future[DocumentsReceived] =
    mainActor.ask(GetUsersDocuments(id, start, limit, _))

  def postDocument(
      userId: String,
      publicName: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[Files]]
  ): Future[ActionPerformed] =
    mainActor.ask(PostDocument(userId, publicName, latLng, center, zoom, message, categories, files, _))
  //  def updateDocument(id: String, is: InputStream): Future[ActionPerformed] = mainActor.ask(PostDocument(id, is, _))
  def deleteDocument(id: String): Future[ActionPerformed] = mainActor.ask(DeleteDocument(id, _))

  def postComment(docId: String, commentator: String, publicName: String, text: String): Future[ActionPerformed] =
    mainActor.ask(PostComment(docId, commentator, publicName, text, _))
  def getComment(commentId: String): Future[CommentReceived] = mainActor.ask(GetComment(commentId, _))

  def getDocumentComments(docId: String, start: Int, limit: Int): Future[CommentsReceived] =
    mainActor.ask(GetDocumentComments(docId, start, limit, _))

  def getUserComments(userId: String, start: Int, limit: Int): Future[CommentsReceived] =
    mainActor.ask(GetUserComments(userId, start, limit, _))

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
          pathPrefix("single") {
            get {
              parameter("id") { id =>
                onSuccess(getDocument(id)) { documentReceived =>
                  complete(StatusCodes.Accepted, documentReceived)
                }
              }
            }
          },
          pathPrefix("all") {
            get {
              parameters("start".as[Int], "limit".as[Int]) { (start, limit) =>
                onSuccess(getAllDocuments(start, limit)) { documentsReceived =>
                  complete(StatusCodes.Accepted, documentsReceived)
                }
              }
            }
          },
          pathPrefix("user") {
            parameters("privateName".as[String], "start".as[Int], "limit".as[Int]) { (id, start, limit) =>
              get {
                onSuccess(getUsersDocuments(id, start, limit)) { documentsReceived =>
                  complete(StatusCodes.Accepted, documentsReceived)
                }
              }
            }
          },
          post {
            entity(as[DocumentToReceive]) { doc =>
              onSuccess(
                postDocument(doc.userId,
                             doc.publicName,
                             doc.latLng,
                             doc.center,
                             doc.zoom,
                             doc.message,
                             doc.categories,
                             doc.files)
              ) { actionPerformed =>
                complete(StatusCodes.Created, actionPerformed)
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
      },
      pathPrefix("feedback") {
        concat(
          pathPrefix("single") {
            get {
              parameter("commentId".as[String]) { commentId =>
                onSuccess(getComment(commentId)) { actionPerformed =>
                  complete(StatusCodes.OK, actionPerformed)
                }
              }
            }
          },
          pathPrefix("document") {
            get {
              parameter("docId".as[String], "start".as[Int], "limit".as[Int]) { (docId, start, limit) =>
                onSuccess(getDocumentComments(docId, start, limit)) { actionPerformed =>
                  complete(StatusCodes.OK, actionPerformed)
                }
              }
            }
          },
          pathPrefix("user") {
            get {
              parameter("userId".as[String], "start".as[Int], "limit".as[Int]) { (userId, start, limit) =>
                onSuccess(getUserComments(userId, start, limit)) { actionPerformed =>
                  complete(StatusCodes.OK, actionPerformed)
                }
              }
            }
          },
          post {
            entity(as[Comment]) { comment =>
              onSuccess(postComment(comment.docId, comment.userId, comment.publicName, comment.text)) { actionPerformed =>
                complete(StatusCodes.OK, actionPerformed)
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
