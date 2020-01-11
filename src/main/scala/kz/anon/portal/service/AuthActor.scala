package kz.anon.portal.service

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import kz.anon.portal.serializer.ElasticJson

import scala.util.{Failure, Success}

final case class User(phoneNumber: String, paswword: String)

object AuthActor extends ElasticJson {

  sealed trait Command
  final case class GetUser(id: String, replyTo: ActorRef[ActionPerformed])    extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class ActionPerformed(description: String)

  def apply(elasticClient: ElasticClient, indexName: String, system: ActorSystem[_]): Behavior[Command] =
    Behaviors.receiveMessage {

      case GetUser(id, replyTo) =>
        import system.executionContext
        elasticClient.execute {
          get(indexName, id)
        }.onComplete {
          case Success(value) =>
            replyTo ! ActionPerformed(s"User: $value successfully found")
          case Failure(exception) =>
            replyTo ! ActionPerformed(s"Failed to find a user")
        }
        Behaviors.same

      case CreateUser(user, replyto) =>
        import system.executionContext
        elasticClient.execute {
          indexInto(indexName).doc(user).id(user.phoneNumber)
        }.onComplete {
          case Success(value) =>
            replyto ! ActionPerformed(s"User: $value created")
          case Failure(exception) =>
            replyto ! ActionPerformed(s"Failed to save into elastic")
        }
        Behaviors.same
    }
}
