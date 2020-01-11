package kz.anon.portal.serializer

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import org.json4s.native.Serialization.write
import org.json4s.native.JsonMethods.parse
import kz.anon.portal.service.User

import scala.util.Try

trait ElasticJson extends Json4sSerializer {

  implicit object ReceivedUser extends Indexable[User] {
    override def json(t: User): String = write(t)
  }

//  implicit object ReceivedUser extends HitReader[User] {
//    override def read(hit: Hit): Either[Throwable, User] =
//      Try(parse(hit.sourceAsString).extract[User]).toEither
//  }
}
