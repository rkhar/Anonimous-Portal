package kz.anon.portal.signup.login.api.serializer

import org.json4s.{jackson, DefaultFormats}

trait Json4sSerializer {
  implicit val serialization = jackson.Serialization
  implicit val formats       = DefaultFormats
}
