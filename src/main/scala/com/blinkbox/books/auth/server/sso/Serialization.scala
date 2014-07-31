package com.blinkbox.books.auth.server.sso

import org.json4s._
import spray.httpx.Json4sJacksonSupport

object Serialization extends Json4sJacksonSupport {

  def camelToUnderscores(name: String) = """[A-Z\d]""".r.replaceAllIn(name, {m =>
      "_" + m.group(0).toLowerCase()
  })

  def underscoreToCamel(name: String) = """_([a-z\d])""".r.replaceAllIn(name, {m =>
      m.group(1).toUpperCase()
  })

  val snakeizer = FieldSerializer[AnyRef](
    deserializer = { case JField(name, v) => JField(underscoreToCamel(name.toLowerCase()), v) },
    serializer = { case (name, v) => Some(camelToUnderscores(name), v) }
  )

  implicit val json4sJacksonFormats: Formats = DefaultFormats + snakeizer
}

