package kz.anon.portal.service

import com.sksamuel.elastic4s.{ElasticClient, Response}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.delete.DeleteResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import kz.anon.portal.serializer.ElasticJson
import kz.anon.portal.service.MainActor.{DocumentToSave, User, UserReceived}
import com.roundeights.hasher.Hasher
import org.slf4j.LoggerFactory
import java.util.UUID.randomUUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ElasticFunctionality(elasticClient: ElasticClient, usersIndex: String, documentsIndex: String)(
    implicit executionContext: ExecutionContext
) extends ElasticJson {

  private val log = LoggerFactory.getLogger("ElasticFuncs")

  def getUser(id: String): Future[Option[User]] = {
    log.info("Getting user by its id")

    elasticClient.execute {
      search(usersIndex).query(idsQuery(id))
    }.map { x =>
      x.map(response => response.to[User])
    }.map(_.toOption).map(z => z.flatMap(_.headOption))
  }

  def createUser(user: User): Future[Response[IndexResponse]] = {
    log.info(s"Creating user with id: ${user.phoneNumber}")

    val userWithHashedPass = user.copy(password = Hasher(user.password).sha256)

    elasticClient.execute {
      indexInto(usersIndex).doc(userWithHashedPass).id(user.phoneNumber)
    }
  }

  def updateUser(user: User): Future[Response[UpdateResponse]] = {
    log.info(s"Updating user with id: ${user.phoneNumber}")

    elasticClient.execute {
      update(user.phoneNumber).in(usersIndex).doc("phoneNumber" -> user.phoneNumber, "password" -> user.password)
    }
  }

  def deleteUser(id: String): Future[Response[DeleteResponse]] = {
    log.info(s"Deleting user with id: $id")

    elasticClient.execute {
      deleteById(usersIndex, id)
    }
  }

  def getDocument(docId: String): Future[Option[DocumentToSave]] = {
    log.info(s"Getting document with id: $docId")

    elasticClient.execute {
      search(documentsIndex).query(idsQuery(docId))
    }.map { x =>
      x.map(
        response => response.to[DocumentToSave]
      )
    }.map(_.toOption).map(z => z.flatMap(_.headOption))
  }

//  def postDocument(id: String, document: String): Future[Response[IndexResponse]] =
//    elasticClient.execute {
//      indexInto(documentsIndex).doc(document).id(id)
//    }

  def postDocument(
      userId: String = "anonymous",
      latLng: List[Double],
      center: List[Double],
      zoom: Int,
      message: String,
      categories: List[String],
      files: Option[List[String]]
  ): Future[Response[IndexResponse]] = {

    val docId = randomUUID.toString
    val doc = DocumentToSave(userId, docId, latLng, center, zoom, message, categories, files)

    elasticClient.execute {
      indexInto(documentsIndex).doc(doc).id(docId)
    }
  }

//  def updateDocument(document: Document): Future[Response[UpdateResponse]] = {
//    log.info(s"Updating document with id: ${document.id}")
//
//    elasticClient.execute {
//      update(document.id).in(usersIndex).doc("data" -> document.data)
//    }
//  }

  def deleteDocument(id: String): Future[Response[DeleteResponse]] =
    elasticClient.execute {
      deleteById(documentsIndex, id)
    }
}
