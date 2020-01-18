package kz.anon.portal.service

import java.io.InputStream
import java.util.Base64

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.sksamuel.elastic4s.ElasticClient
import kz.anon.portal.serializer.ElasticJson
import kz.anon.portal.service.MainActor.{
  ActionPerformed,
  Command,
  CreateUser,
  DeleteUser,
  Document,
  DocumentReceived,
  GetDocument,
  GetUser,
  PostDocument,
  UpdateDocument,
  UpdateUser,
  UserReceived
}

import scala.concurrent.ExecutionContextExecutor

object MainActor {

  sealed trait Command

  final case class User(phoneNumber: String, password: String)
  final case class Document(id: String, data: String)

  final case class UserReceived(description: String, reply: Option[User])
  final case class DocumentReceived(description: String, reply: Option[Document])

  final case class GetUser(id: String, replyTo: ActorRef[UserReceived])       extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class DeleteUser(id: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetDocument(id: String, replyTo: ActorRef[DocumentReceived])                    extends Command
  final case class PostDocument(id: String, is: InputStream, replyTo: ActorRef[ActionPerformed])   extends Command
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

      case GetDocument(id, replyTo) =>
        elasticFuncs
          .getDocument(id)
          .map(x => replyTo ! DocumentReceived("Document successfully received!", x))
          .recover {
            case _: Exception => replyTo ! DocumentReceived("Document not found!", None)
          }

        Behaviors.same

      case PostDocument(id, inputStream, replyTo) =>
        elasticFuncs
          .postDocument(id, isToBase64Str(inputStream))
          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
          }
        Behaviors.same

      case UpdateDocument(id, inputStream, replyTo) =>
        elasticFuncs
          .updateDocument(Document(id, isToBase64Str(inputStream)))
          .map(_ => replyTo ! ActionPerformed(200, "Document has posted!"))
          .recover {
            case _: Exception => replyTo ! ActionPerformed(404, "Document not found!")
          }
        Behaviors.same

      case DeleteUser(id, replyTo) =>
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

}
