package kz.anon.portal.service

import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Base64

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.model.StatusCode
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import javax.swing.text.DefaultEditorKit.BeepAction
import kz.anon.portal.serializer.ElasticJson
import kz.anon.portal.service.MainActor.{
  ActionPerformed,
  Command,
  CreateUser,
  DeleteUser,
  DocumentReceived,
  GetDocument,
  GetUser,
  PostDocument,
  UserReceived
}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object MainActor {

  sealed trait Command

  final case class User(phoneNumber: String, password: String)

  final case class Document(data: String)

  final case class UserReceived(description: String, reply: Option[User])

  final case class DocumentReceived(description: String, reply: Option[Document])

  final case class GetUser(id: String, replyTo: ActorRef[UserReceived]) extends Command

  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class DeleteUser(id: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetDocument(id: String, replyTo: ActorRef[DocumentReceived]) extends Command

  final case class PostDocument(id: String, inputStream: InputStream, replyTo: ActorRef[ActionPerformed])
      extends Command

  final case class DeleteDocument(id: String, replyTo: ActorRef[ActionPerformed]) extends Command

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
            case e: Exception => replyTo ! UserReceived("User not found!", None)
          }

        Behaviors.same

      case CreateUser(user, replyTo) =>
        elasticFuncs
          .createUser(user)
          .map(_ => replyTo ! ActionPerformed(201, "User successfully created!"))
          .recover {
            case e: Exception => replyTo ! ActionPerformed(404, "User not found!")
          }
        Behaviors.same

      case DeleteUser(id, replyTo) =>
        elasticFuncs
          .deleteUser(id)
          .map(_ => replyTo ! ActionPerformed(200, "User successfully deleted!"))
          .recover {
            case e: Exception => replyTo ! ActionPerformed(404, "User not found!")
          }
        Behaviors.same

      case GetDocument(id, replyTo) =>
        elasticFuncs
          .getDocument(id)
          .map(x => replyTo ! DocumentReceived("Document successfully received!", x))
          .recover {
            case e: Exception => replyTo ! DocumentReceived("Document not found!", None)
          }

        Behaviors.same

      case PostDocument(id, inputStream, replyTo) =>
        val arrayByte = LazyList.continually(inputStream.read()).takeWhile(_ != -1).map(_.toByte).toArray
        val img       = Base64.getEncoder.encodeToString(arrayByte)

        elasticFuncs
          .postDocument(id, img)
          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
          .recover {
            case e: Exception => replyTo ! ActionPerformed(404, "Document not found!")
          }
        replyTo ! ActionPerformed(200, img)
        Behaviors.same

      case DeleteUser(id, replyTo) =>
        elasticFuncs
          .deleteDocument(id)
          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
          .recover {
            case e: Exception => replyTo ! ActionPerformed(404, "Document not found!")
          }
        Behaviors.same
    }

}
