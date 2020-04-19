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

object MainActor {

  sealed trait Command

  final case class UpdateUserModel(privateName: String, password: String)
  final case class User(privateName: String, publicName: String = "Anonymous", password: String)

  final case class DocumentToSave(
      userId: String,
      docId: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[String]]
  )

  final case class DocumentToReceive(
      userId: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[String]]
  )

  final case class UserReceived(description: String, reply: Option[User])
  final case class DocumentReceived(description: String, reply: Option[DocumentToSave])

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

  final case class GetDocument(id: String, replyTo: ActorRef[DocumentReceived]) extends Command
  final case class Files(name: String, content: String)

  final case class PostDocument(
      userId: String,
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[String]],
      replyTo: ActorRef[ActionPerformed]
  ) extends Command
//  final case class PostDocument(
//                                 email: Option[String] = Some("anonymous"),
//                                 latLng: Double,
//                                 center: Double,
//                                 zoom: Int,
//                                 message: String,
//                                 files: List[],
//                                   is: InputStream,
//  replyTo: ActorRef[ActionPerformed]
//  ) extends Command
  final case class UpdateDocument(id: String, is: InputStream, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class DeleteDocument(id: String, replyTo: ActorRef[ActionPerformed])                  extends Command

  final case class ActionPerformed(statusCode: Int, description: String)

  final case class TokenResponse(
      statusCode: Int,
      description: String,
      jwtToken: Option[String] = None,
      publicName: Option[String] = None
  )

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
            .getUserById(id)
            .map {
              case Some(value) =>
                context.log.info(s"User successfully found!: $value")
                replyTo ! UserReceived("User successfully found!", Some(value))
              case None =>
                context.log.info(s"User not found!")
                replyTo ! UserReceived("User not found!", None)
            }
            .recover {
              case exception: Exception =>
                context.log.error(s"exception: $exception")
                replyTo ! UserReceived("User not found!", None)

            }
        } else {
          context.log.info(s"User not found!")
          replyTo ! UserReceived("User not found!", None)
        }

        Behaviors.same

      case CreateUser(user, replyTo) =>
        elasticFuncs
          .getUserById(user.privateName)
          .map {
            case Some(_) =>
              replyTo ! TokenResponse(201, "User already exists")
            case None =>
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
          .recover {
            case _: Exception =>
              replyTo ! TokenResponse(404, "User not found!")
          }

        Behaviors.same

      case UpdateUser(headers, id, password, replyTo) =>
        if (checkToken(headers)) {
          elasticFuncs.getUserById(id).map {
            case Some(_) =>
              val hashedPassword = Hasher(password).sha256.hash
              elasticFuncs
                .updateUser(id, hashedPassword)
                .map(_ => replyTo ! ActionPerformed(200, "User successfully updated!"))
                .recover {
                  case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
                }
            case None =>
              replyTo ! ActionPerformed(404, "User not found!")
          }
        } else {
          context.log.info(s"User not found!")
          replyTo ! ActionPerformed(404, "User not found!")
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
          context.log.info(s"User not found!")
          replyTo ! ActionPerformed(404, "User not found!")
        }

        Behaviors.same

      case Login(privateName, password, replyTo) =>
        val userWithHashedPass = Hasher(password).sha256.hash

        elasticFuncs
          .getUserById(privateName)
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
              replyTo ! DocumentReceived("Document successfully received!", Some(value))
            case None =>
              replyTo ! DocumentReceived("Document not found!", None)
          }
          .recover {
            case _: Exception => replyTo ! DocumentReceived("Document not found!", None)
          }

        Behaviors.same

//      case PostDocument(id, inputStream, replyTo) =>
//        elasticFuncs
//          .postDocument(id, isToBase64Str(inputStream))
//          .map(_ => replyTo ! ActionPerformed(200, "Document was posted!"))
//          .recover {
//            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
//          }
//        Behaviors.same

      case PostDocument(userId, latLng, center, zoom, message, categories, files, replyTo) =>
        elasticFuncs.getUserById(userId).map {
          case Some(_) =>
            elasticFuncs
              .postDocument(userId, latLng, center, zoom, message, categories, files)
              .map(_ => replyTo ! ActionPerformed(200, "Document was posted!"))
              .recover {
                case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
              }
          case None =>
            replyTo ! ActionPerformed(404, "User not found!")
        }

        Behaviors.same

//      case UpdateDocument(id, inputStream, replyTo) =>
//        elasticFuncs
//          .updateDocument(Document(id, isToBase64Str(inputStream)))
//          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
//          .recover {
//            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
//          }
//        Behaviors.same

      case DeleteDocument(id, replyTo) =>
        elasticFuncs
          .deleteDocument(id)
          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
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
    val token     = headers.getOrElse("Authorization", "").split(" ")(1)
    val key       = "secretKey"
    val algorithm = JwtAlgorithm.HS256
    Jwt.decode(token, key, Seq(algorithm)).isSuccess
  }

}
