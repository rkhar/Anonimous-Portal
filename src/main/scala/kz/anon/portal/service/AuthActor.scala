package kz.anon.portal.service

import java.time.LocalDate

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.sksamuel.elastic4s.ElasticClient

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

final case class User(name: String, age: Int, countryOfResidence: String)
final case class Users(users: immutable.Seq[User])

object AuthActor {

  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command

  def apply(users: Set[User]): Behavior[Command] = Behaviors.receiveMessage {
    case GetUsers(replyTo) =>
      replyTo ! Users(users.toSeq)
      Behaviors.same
  }

  //      elasticClient
  //        .execute {
  //          indexInto(indexName).doc(receivedRegistrationData).id(phoneNumber)
  //        }
  //        .onComplete {
  //          case Success(value) =>
  //            log.info("Data: {} was inserted", value)
  //            context.parent !
  //              HttpResponse(StatusCodes.OK, entity = "Nice!")
  //          case Failure(exception) =>
  //            log.error(exception, "Failed to save into elastic")
  //            context.parent ! exception
  //        }

}
