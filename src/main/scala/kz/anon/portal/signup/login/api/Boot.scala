package kz.anon.portal.signup.login.api

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import akka.http.scaladsl.server.Directives._
import kz.anon.portal.signup.login.api.mainapi.MainApi
import kz.anon.portal.signup.login.api.service.RegistrationActor

import scala.concurrent.ExecutionContextExecutor

object Boot extends App with MainApi {

  implicit val system: ActorSystem                        = ActorSystem("actor-system")
  implicit val materializer: ActorMaterializer            = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val settings = CorsSettings.defaultSettings

//  val routes = cors(settings){
//    implicit val routerBlockingDispatcher: MessageDispatcher = system.dispatchers.lookup("router-dispatcher")
  val routes =
    concat(
      pathPrefix("main") {
        concat(
          mainRoutes
        )
      } ~
        path("healthcheck") {
          complete(HttpResponse(entity = "Got ya!"))
        }
    )
//  }

  def registrationProps = RegistrationActor.props()

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)
}
