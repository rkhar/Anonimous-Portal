package kz.anon.portal.service

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, RequestEntity}
import kz.anon.portal.serializer.Json4sSerializer
import kz.anon.portal.service.MainActor.DocumentToCheck
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

case class HttpClient(uri: String)(
    implicit system: akka.actor.ActorSystem,
    executionContext: ExecutionContextExecutor
) extends Json4sSerializer {
  private val log = LoggerFactory.getLogger("ExternalServiceFuncs")

  def checkDocument(documentToCheck: DocumentToCheck): Future[HttpResponse] =
    Marshal(documentToCheck).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(HttpMethods.POST, uri, List(), entity)
      log.debug(s"Requesting external uri to check doc where request is: $request and entity is: $entity")
      Http().singleRequest(request)

    }

}
