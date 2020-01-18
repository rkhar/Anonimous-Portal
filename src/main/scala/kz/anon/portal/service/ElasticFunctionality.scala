package kz.anon.portal.service

import com.sksamuel.elastic4s.{ElasticClient, Response}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.delete.DeleteResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import kz.anon.portal.serializer.ElasticJson
import kz.anon.portal.service.MainActor.{Document, User, UserReceived}
import org.slf4j.LoggerFactory

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

    elasticClient.execute {
      indexInto(usersIndex).doc(user).id(user.phoneNumber)
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

  def getDocument(id: String): Future[Option[Document]] = {
    log.info(s"Getting document with id: $id")

    elasticClient.execute {
      search(documentsIndex).query(idsQuery(id))
    }.map { x =>
      x.map(
        response => response.to[Document]
      )
    }.map(_.toOption).map(z => z.flatMap(_.headOption))
  }

  def postDocument(id: String, document: String): Future[Response[IndexResponse]] =
    elasticClient.execute {
      indexInto(documentsIndex).doc(document).id(id)
    }

  def updateDocument(document: Document): Future[Response[UpdateResponse]] = {
    log.info(s"Updating document with id: ${document.id}")

    elasticClient.execute {
      update(document.id).in(usersIndex).doc("data" -> document.data)
    }
  }

  def deleteDocument(id: String): Future[Response[DeleteResponse]] =
    elasticClient.execute {
      deleteById(documentsIndex, id)
    }
}
