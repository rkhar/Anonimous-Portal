package kz.anon.portal.service

import java.io.InputStream
import java.util.Base64

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.roundeights.hasher.Hasher
import kz.anon.portal.serializer.ElasticJson
import kz.anon.portal.service.MainActor._
import org.joda.time.DateTime
import org.json4s.JsonDSL.WithBigDecimal._
import org.json4s._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtJson4s}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object MainActor {

  sealed trait Command

  final case class UpdateUserModel(privateName: String, password: String)
  final case class User(privateName: String, publicName: String = "Anonymous", password: String)
  final case class Files(name: String, content: String)

  final case class DocumentToSave(
      userId: String,
      publicName: String,
      docId: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[Files]],
      date: Long
  )

  final case class ShortDocumentInfo(
      docId: String,
      publicName: String,
      message: String,
      categories: List[String],
      date: Long
  )

  final case class DocsWithCount(count: Long, shortDocumentInfo: IndexedSeq[ShortDocumentInfo])
  final case class CommentsWithCount(count: Long, comments: IndexedSeq[CommentToSave])

  final case class DocumentToReceive(
      userId: String,
      publicName: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[Files]]
  )
  final case class Comment(docId: String, commentator: String, text: String)
  final case class CommentToSave(commentID: String, docId: String, commentator: String, text: String, date: Long)

  final case class TokenResponse(
      statusCode: Int,
      description: String,
      jwtToken: Option[String] = None,
      publicName: Option[String] = None
  )

  final case class UserReceived(description: String, reply: Option[User])
  final case class DocumentReceived(description: String, reply: Option[DocumentToSave])
  final case class CommentReceived(description: String, reply: Option[CommentToSave])
  final case class DocumentsReceived(description: String, reply: Option[DocsWithCount])
  final case class CommentsReceived(description: String, reply: Option[CommentsWithCount])
  final case class GetUser(headers: Map[String, String], id: String, replyTo: ActorRef[UserReceived]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[TokenResponse])                           extends Command

  final case class UpdateUser(
      headers: Map[String, String],
      id: String,
      password: String,
      replyTo: ActorRef[ActionPerformed]
  ) extends Command

  final case class DeleteUser(headers: Map[String, String], id: String, replyTo: ActorRef[ActionPerformed])
      extends Command
  final case class Login(privateNumber: String, password: String, replyTo: ActorRef[TokenResponse]) extends Command
  final case class GetDocument(id: String, replyTo: ActorRef[DocumentReceived])                     extends Command
  final case class GetAllDocuments(start: Int, limit: Int, replyTo: ActorRef[DocumentsReceived])    extends Command

  final case class GetUsersDocuments(id: String, start: Int, limit: Int, replyTo: ActorRef[DocumentsReceived])
      extends Command

  final case class PostDocument(
      userId: String,
      publicName: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[Files]],
      replyTo: ActorRef[ActionPerformed]
  ) extends Command

  final case class UpdateDocument(id: String, is: InputStream, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class DeleteDocument(id: String, replyTo: ActorRef[ActionPerformed])                  extends Command

  final case class PostComment(docId: String, commentator: String, text: String, replyTo: ActorRef[ActionPerformed])
      extends Command
  final case class GetComment(commentId: String, replyTo: ActorRef[CommentReceived]) extends Command

  final case class GetDocumentComments(docId: String, start: Int, limit: Int, replyTo: ActorRef[CommentsReceived])
      extends Command

  final case class GetUserComments(userId: String, start: Int, limit: Int, replyTo: ActorRef[CommentsReceived])
      extends Command
  final case class ActionPerformed(statusCode: Int, description: String)

  def apply(elasticFuncs: ElasticFunctionality): Behavior[Command] =
    Behaviors.setup(context => new MainActor(context, elasticFuncs))
}

class MainActor(
    context: ActorContext[Command],
    elasticFuncs: ElasticFunctionality
) extends AbstractBehavior[Command](context)
    with ElasticJson {
  implicit val executionContext: ExecutionContextExecutor = context.executionContext

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {

      case GetUser(headers, id, replyTo) =>
        if (checkToken(headers)) {
          elasticFuncs
            .getUserByPrivateName(id)
            .map {
              case Some(value) =>
                replyTo ! UserReceived("User successfully found!", Some(value))
              case None =>
                replyTo ! UserReceived("User not found!", None)
            }
            .recover {
              case _: Exception =>
                replyTo ! UserReceived("User not found!", None)

            }
        } else {
          replyTo ! UserReceived("Invalid Token!", None)
        }

        Behaviors.same

      case CreateUser(user, replyTo) =>
        elasticFuncs
          .ifUserExists(user.privateName)
          .map { res =>
            if (res) replyTo ! TokenResponse(201, "User already exists")
            else {
              val hashedUser = user.copy(publicName = Hasher(user.privateName + DateTime.now.toString).sha256.hash)
              elasticFuncs
                .createUser(hashedUser)
                .map(
                  _ =>
                    replyTo ! TokenResponse(201,
                                            "User successfully created!",
                                            Some(tokenGenerate(hashedUser.publicName, hashedUser.password)),
                                            Some(hashedUser.publicName))
                )
                .recover {
                  case _: Exception => replyTo ! TokenResponse(404, "User can not be created!")
                }
            }
          }
          .recover {
            case _: Exception => replyTo ! TokenResponse(404, "User can not be created!")
          }

        Behaviors.same

      case UpdateUser(headers, id, password, replyTo) =>
        if (checkToken(headers)) {
          elasticFuncs.ifUserExists(id).map { res =>
            if (res) {
              val hashedPassword = Hasher(password).sha256.hash
              elasticFuncs
                .updateUser(id, hashedPassword)
                .map(_ => replyTo ! ActionPerformed(200, "User successfully updated!"))
                .recover {
                  case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
                }
            } else
              replyTo ! ActionPerformed(404, "User not found!")
          }
        } else {
          replyTo ! ActionPerformed(403, "Invalid Token!")
        }

        Behaviors.same

      case DeleteUser(headers, id, replyTo) =>
        if (checkToken(headers)) {
          elasticFuncs
            .deleteUser(id)
            .map(_ => replyTo ! ActionPerformed(200, "User successfully deleted!"))
            .recover {
              case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
            }
        } else {
          replyTo ! ActionPerformed(403, "Invalid Token!")
        }

        Behaviors.same

      case Login(privateName, password, replyTo) =>
        val userWithHashedPass = Hasher(password).sha256.hash

        elasticFuncs
          .getUserByPrivateName(privateName)
          .map {
            case Some(value) =>
              if (value.password == userWithHashedPass.toString)
                replyTo ! TokenResponse(200,
                                        "Successfully logged in!",
                                        Some(tokenGenerate(privateName, password)),
                                        Some(value.publicName))
              else
                replyTo ! TokenResponse(404, "Wrong password!")
            case None =>
              replyTo ! TokenResponse(404, "User not found!")
          }
          .recover {
            case _: Exception => replyTo ! TokenResponse(404, "User not found!")
          }

        Behaviors.same

      case GetDocument(docId, replyTo) =>
        elasticFuncs
          .getDocument(docId)
          .map {
            case Some(value) =>
              val anonDoc = value.copy(userId = "Anonymous")
              replyTo ! DocumentReceived("Document successfully received!", Some(anonDoc))
            case None =>
              replyTo ! DocumentReceived("Document not found!", None)
          }
          .recover {
            case _: Exception => replyTo ! DocumentReceived("Document not found!", None)
          }

        Behaviors.same

      case GetAllDocuments(start, limit, replyTo) =>
        elasticFuncs
          .getAllDocuments(start, limit)
          .map {
            case Some(value) =>
              elasticFuncs.countAllDocuments.onComplete {
                case Success(count) =>
                  val pages         = count / (limit - start) + 1
                  val docsWithCount = DocsWithCount(pages, value)
                  replyTo ! DocumentsReceived("Documents successfully received!", Some(docsWithCount))
                case Failure(_) =>
                  replyTo ! DocumentsReceived("Documents not found!", None)
              }
            case None =>
              replyTo ! DocumentsReceived("Documents not found!", None)

          }
          .recover {
            case _: Exception => replyTo ! DocumentsReceived("Documents not found!", None)
          }

        Behaviors.same

      case GetUsersDocuments(id, start, limit, replyTo) =>
        elasticFuncs
          .getUsersDocuments(id, start, limit)
          .map {
            case Some(value) =>
              elasticFuncs.countUsersDocuments(id).onComplete {
                case Success(count) =>
                  val pages         = count / (limit - start) + 1
                  val docsWithCount = DocsWithCount(pages, value)
                  replyTo ! DocumentsReceived("Documents successfully received!", Some(docsWithCount))
                case Failure(_) =>
                  replyTo ! DocumentsReceived("Documents not found!", None)
              }
            case None =>
              replyTo ! DocumentsReceived("Documents not found!", None)
          }
          .recover {
            case _: Exception => replyTo ! DocumentsReceived("Documents not found!", None)
          }

        Behaviors.same

      case PostDocument(userId, publicName, latLng, center, zoom, message, categories, files, replyTo) =>
        elasticFuncs.ifUserExists(userId).map { res =>
          if (res) {
            val date = DateTime.now.getMillis
            elasticFuncs
              .postDocument(userId, publicName, latLng, center, zoom, message, categories, files, date)
              .map(_ => replyTo ! ActionPerformed(200, "Document was posted!"))
              .recover {
                case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
              }
          } else
            replyTo ! ActionPerformed(404, "User not found!")
        }

        Behaviors.same

      case DeleteDocument(id, replyTo) =>
        elasticFuncs
          .deleteDocument(id)
          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
          }

        Behaviors.same

      case PostComment(docId, commentator, text, replyTo) =>
        elasticFuncs.ifUserExists(commentator).map { userRes =>
          if (userRes) {
            elasticFuncs
              .ifDocExists(docId)
              .map { docRes =>
                if (docRes) {
                  val date = DateTime.now.getMillis
                  elasticFuncs
                    .postComment(docId, commentator, text, date)
                    .map(_ => replyTo ! ActionPerformed(200, "Comment was posted!"))
                    .recover {
                      case _: Exception => replyTo ! ActionPerformed(404, "Comment is invalid!")
                    }
                } else
                  replyTo ! ActionPerformed(404, "Document not found!")
              }
              .recover {
                case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
              }
          } else
            replyTo ! ActionPerformed(404, "User not found!")
        }

        Behaviors.same

      case GetComment(commentId, replyTo) =>
        elasticFuncs
          .getComment(commentId)
          .map {
            case Some(value) =>
              replyTo ! CommentReceived("Comment successfully received!", Some(value))
            case None =>
              replyTo ! CommentReceived("Comment not found!", None)
          }
          .recover {
            case _: Exception => replyTo ! CommentReceived("Comment not found!", None)
          }

        Behaviors.same

      case GetDocumentComments(docId, start, limit, replyTo) =>
        elasticFuncs
          .getDocumentComments(docId, start, limit)
          .map {
            case Some(value) =>
              elasticFuncs.countDocumentComments(docId).onComplete {
                case Success(count) =>
                  val pages             = count / (limit - start) + 1
                  val commentsWithCount = CommentsWithCount(pages, value)
                  replyTo ! CommentsReceived("Comments successfully received!", Some(commentsWithCount))
                case Failure(_) =>
                  replyTo ! CommentsReceived("Comments not found!", None)
              }
            case None =>
              replyTo ! CommentsReceived("Comments not found!", None)
          }
          .recover {
            case _: Exception => replyTo ! CommentsReceived("Comments not found!", None)
          }

        Behaviors.same

      case GetUserComments(userId, start, limit, replyTo) =>
        elasticFuncs
          .getUsersComments(userId, start, limit)
          .map {
            case Some(value) =>
              elasticFuncs.countUsersComments(userId).onComplete {
                case Success(count) =>
                  val pages             = count / (limit - start) + 1
                  val commentsWithCount = CommentsWithCount(pages, value)
                  replyTo ! CommentsReceived("Comments successfully received!", Some(commentsWithCount))
                case Failure(_) =>
                  replyTo ! CommentsReceived("Comments not found!", None)
              }
            case None =>
              replyTo ! CommentsReceived("Comments not found!", None)
          }
          .recover {
            case _: Exception => replyTo ! CommentsReceived("Comments not found!", None)
          }

        Behaviors.same
    }

  private def isToBase64Str(inputStream: InputStream): String = {
    val arrayByte = LazyList.continually(inputStream.read()).takeWhile(_ != -1).map(_.toByte).toArray
    Base64.getEncoder.encodeToString(arrayByte)
  }

  private def tokenGenerate(privateName: String, password: String): String = {
    val claim     = JObject(("privateName", privateName), ("password", password))
    val key       = "secretKey"
    val algorithm = JwtAlgorithm.HS256
    JwtJson4s.encode(claim, key, algorithm)
  }

  def checkToken(headers: Map[String, String]): Boolean = {
    val bearerToken = headers.getOrElse("Authorization", "")
    val token       = if (bearerToken.nonEmpty) bearerToken.split(" ")(1) else ""
    val key         = "secretKey"
    val algorithm   = JwtAlgorithm.HS256
    Jwt.decode(token, key, Seq(algorithm)).isSuccess
  }

}
