package kz.anon.portal.serializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, jackson}

trait Json4sSerializer extends Json4sSupport{
  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: DefaultFormats.type      = DefaultFormats
}
