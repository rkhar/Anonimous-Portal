package kz.anon.portal.serializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.anon.portal.service.User
import org.json4s.jackson.Serialization
import org.json4s.{jackson, DefaultFormats}
import org.json4s.{Formats, Serialization, ShortTypeHints}

trait Json4sSerializer extends Json4sSupport {
  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: Formats                  = Serialization.formats(ShortTypeHints(List(classOf[User])))
}
