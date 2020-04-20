package kz.anon.portal.serializer

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import kz.anon.portal.service.MainActor.{DocsWithCount, DocumentToSave, ShortDocumentInfo, User}
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

  implicit object ReceivedDocumentIndexable extends Indexable[DocumentToSave] {
    override def json(t: DocumentToSave): String = write(t)
  }

  implicit object DocumentHitReader extends HitReader[DocumentToSave] {
    override def read(hit: Hit): Try[DocumentToSave] = Try(parse(hit.sourceAsString).extract[DocumentToSave])
  }

  implicit object ReceivedShortDocumentInfoIndexable extends Indexable[ShortDocumentInfo] {
    override def json(t: ShortDocumentInfo): String = write(t)
  }

  implicit object ShortDocumentInfoHitReader extends HitReader[ShortDocumentInfo] {
    override def read(hit: Hit): Try[ShortDocumentInfo] = Try(parse(hit.sourceAsString).extract[ShortDocumentInfo])
  }

//  implicit object ReceivedDocsWithCountIndexable extends Indexable[DocsWithCount] {
//    override def json(t: DocsWithCount): String = write(t)
//  }
//
//  implicit object DocsWithCountHitReader extends HitReader[DocsWithCount] {
//    override def read(hit: Hit): Try[DocsWithCount] = Try(parse(hit.sourceAsString).extract[DocsWithCount])
//  }

}
