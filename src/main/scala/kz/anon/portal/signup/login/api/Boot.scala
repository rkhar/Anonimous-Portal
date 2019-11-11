package kz.anon.portal.signup.login.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import akka.http.scaladsl.server.Directives._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.typesafe.config.ConfigFactory
import kz.anon.portal.signup.login.api.mainapi.MainApi
import kz.anon.portal.signup.login.api.service.RegistrationActor
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

object Boot extends App with MainApi {
  val log: Logger = LoggerFactory.getLogger("Boot")

  private val config = ConfigFactory.load()

  implicit val system: ActorSystem                = ActorSystem("actor-system")
  implicit val materializer: ActorMaterializer    = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val indexName: String    = config.getString("elastic.indexes.users")
  val elasticHosts: String = config.getString("elastic.hosts")
  val elasticPorts: String = config.getString("elastic.ports")
  val elasticClient: ElasticClient = ElasticClient(
    JavaClient(ElasticProperties(s"http://$elasticHosts:$elasticPorts"))
  )

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

  def registrationProps = RegistrationActor.props(elasticClient)

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  if (!elasticClient
        .execute(indexExists(indexName))
        .await
        .result
        .isExists)
    elasticClient.execute(createIndex(indexName))
  else log.info(s"$indexName already exists")
}
