package kz.anon.portal.serializer

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import kz.anon.portal.service.MainActor.{Document, User}
import org.json4s.native.Serialization.write
import org.json4s.native.JsonMethods.parse

import scala.util.Try

trait ElasticJson extends Json4sSerializer {

  implicit object ReceivedUserIndexable extends Indexable[User] {
    override def json(t: User): String = write(t)
  }

  implicit object UserHitReader extends HitReader[User] {
    override def read(hit: Hit): Try[User] = Try(parse(hit.sourceAsString).extract[User])
  }

  implicit object ReceivedDocumentIndexable extends Indexable[Document] {
    override def json(t: Document): String = write(t)
  }

  implicit object DocumentHitReader extends HitReader[Document] {
    override def read(hit: Hit): Try[Document] = Try(parse(hit.sourceAsString).extract[Document])
  }
}
