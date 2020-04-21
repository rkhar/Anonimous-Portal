package kz.anon.portal

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.typesafe.config.ConfigFactory
import kz.anon.portal.mainapi.MainApi
import kz.anon.portal.service.{ElasticFunctionality, HttpClient, MainActor}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Boot {

  private def startHttpServer(
      routes: Route
  )(implicit system: akka.actor.ActorSystem, executionContext: ExecutionContextExecutor): Unit = {
    val futureBinding = Http().bindAndHandle(routes, "0.0.0.0", 8080)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    //    ---------- ElasticSearch connection creation ----------

    val config = ConfigFactory.load()

    val usersIndex: String     = config.getString("elastic.indexes.users")
    val documentsIndex: String = config.getString("elastic.indexes.documents")
    val commentsIndex: String  = config.getString("elastic.indexes.comments")
    val elasticHosts: String   = config.getString("elastic.hosts")
    val elasticPorts: String   = config.getString("elastic.ports")
    val externalUri: String    = config.getString("uri")
    val elasticClient: ElasticClient = ElasticClient(
      JavaClient(ElasticProperties(s"http://$elasticHosts:$elasticPorts"))
    )
    //    ---------- ElasticSearch connection creation ----------

    //    ---------- server-bootstrapping ----------

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      implicit val classicSystem: akka.actor.ActorSystem      = context.system.toClassic

      val elasticFuncs = new ElasticFunctionality(elasticClient, usersIndex, documentsIndex, commentsIndex)

      val httpClient = HttpClient(externalUri)

      val mainActor =
        context.spawn(MainActor(elasticFuncs, httpClient), "UserRegistryActor")
      context.watch(mainActor)

      val routes = new MainApi(mainActor)(context.system)
      startHttpServer(routes.mainRoutes)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "LocalServer")
    //    ---------- server-bootstrapping ----------

    if (!elasticClient.execute(indexExists(usersIndex)).await.result.isExists)
      elasticClient.execute(createIndex(usersIndex))
    else system.log.info(s"$usersIndex already exists")

    if (!elasticClient.execute(indexExists(documentsIndex)).await.result.isExists)
      elasticClient.execute(createIndex(documentsIndex))
    else system.log.info(s"$documentsIndex already exists")

    if (!elasticClient.execute(indexExists(commentsIndex)).await.result.isExists)
      elasticClient.execute(createIndex(commentsIndex))
    else system.log.info(s"$commentsIndex already exists")
  }

}
