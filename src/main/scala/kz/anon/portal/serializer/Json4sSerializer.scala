package kz.anon.portal.serializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.anon.portal.service.MainActor.{DocumentToSave, Files, User}
import org.json4s.jackson.Serialization
import org.json4s.jackson
import org.json4s.{Formats, ShortTypeHints}

trait Json4sSerializer extends Json4sSupport {
  implicit val serialization: Serialization.type = jackson.Serialization

  implicit val formats: Formats =
    Serialization.formats(ShortTypeHints(List(classOf[User], classOf[DocumentToSave], classOf[Files])))
}
