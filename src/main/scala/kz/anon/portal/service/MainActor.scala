package kz.anon.portal.service

import java.io.InputStream
import java.util.Base64

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.roundeights.hasher.Hasher
import com.sksamuel.elastic4s.ElasticClient
import kz.anon.portal.serializer.ElasticJson
import kz.anon.portal.service.MainActor._
import org.json4s.JsonDSL.WithBigDecimal._
import org.json4s._
import pdi.jwt.{JwtAlgorithm, JwtJson4s}

import scala.concurrent.ExecutionContextExecutor

object MainActor {

  sealed trait Command

  final case class User(phoneNumber: String, password: String)

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

  final case class GetUser(id: String, replyTo: ActorRef[UserReceived])                             extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed])                       extends Command
  final case class UpdateUser(user: User, replyTo: ActorRef[ActionPerformed])                       extends Command
  final case class DeleteUser(id: String, replyTo: ActorRef[ActionPerformed])                       extends Command
  final case class Login(phoneNumber: String, password: String, replyTo: ActorRef[ActionPerformed]) extends Command

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

  def apply(elasticFuncs: ElasticFunctionality, elasticClient: ElasticClient, indexName: String): Behavior[Command] =
    Behaviors.setup(context => new MainActor(context, elasticFuncs, elasticClient, indexName))
}

class MainActor(
    context: ActorContext[Command],
    elasticFuncs: ElasticFunctionality,
    elasticClient: ElasticClient,
    indexName: String
) extends AbstractBehavior[Command](context)
    with ElasticJson {
  implicit val executionContext: ExecutionContextExecutor = context.executionContext

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {

      case GetUser(id, replyTo) =>
        elasticFuncs
          .getUser(id)
          .map(user => replyTo ! UserReceived("User successfully found!", user))
          .recover {
            case _: Exception => replyTo ! UserReceived("User not found!", None)
          }

        Behaviors.same

      case CreateUser(user, replyTo) =>
        elasticFuncs
          .createUser(user)
          .map(_ => replyTo ! ActionPerformed(201, "User successfully created!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
          }
        Behaviors.same

      case UpdateUser(user, replyTo) =>
        elasticFuncs
          .updateUser(user)
          .map(_ => replyTo ! ActionPerformed(200, "User successfully updated!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
          }
        Behaviors.same

      case DeleteUser(id, replyTo) =>
        elasticFuncs
          .deleteUser(id)
          .map(_ => replyTo ! ActionPerformed(200, "User successfully deleted!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
          }
        Behaviors.same

      case Login(phoneNumber, password, replyTo) =>
        val userWithHashedPass = Hasher(password).sha256.hash

        elasticFuncs
          .getUser(phoneNumber)
          .map {
            case Some(value) =>
              if (value.password == userWithHashedPass.toString)
                replyTo ! ActionPerformed(200, tokenGenerate(phoneNumber, password))
              else
                replyTo ! ActionPerformed(404, "Wrong password!")
            case None =>
              replyTo ! ActionPerformed(404, "User not found!")
          }
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "User not found!")
          }
        Behaviors.same

      case GetDocument(docId, replyTo) =>
        elasticFuncs
          .getDocument(docId)
          .map(x => replyTo ! DocumentReceived("Document successfully received!", x))
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
        elasticFuncs
          .postDocument(userId, latLng, center, zoom, message, categories, files)
          .map(_ => replyTo ! ActionPerformed(200, "Document was posted!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
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

  private def tokenGenerate(phoneNumber: String, password: String) = {
    val claim     = JObject(("phoneNumebr", phoneNumber), ("password", password))
    val key       = "secretKey"
    val algorithm = JwtAlgorithm.HS256
    JwtJson4s.encode(claim, key, algorithm)
  }
}
