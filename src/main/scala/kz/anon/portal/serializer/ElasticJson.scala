//package kz.anon.portal.serializer
//
//import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
//import org.json4s.native.Serialization.write
//import org.json4s.native.JsonMethods.parse
//import kz.anon.portal.auth.api.service.RegistrationActor.ReceivedRegistrationData
//
//import scala.util.Try
//
//trait ElasticJson extends Json4sSerializer {
//
//  implicit object ReceivedRegistrationDataIndexible extends Indexable[ReceivedRegistrationData] {
//    override def json(t: ReceivedRegistrationData): String = write(t)
//  }
//
////  implicit object ReceivedRegistrationDataHit extends HitReader[ReceivedRegistrationData] {
////    override def read(hit: Hit): Either[Throwable, ReceivedRegistrationData] =
////      Try(parse(hit.sourceAsString).extract[ReceivedRegistrationData]).toEither
////  }
//}
