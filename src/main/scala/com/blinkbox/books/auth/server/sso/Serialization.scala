package com.blinkbox.books.auth.server.sso

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.auth.server.EnumContainer
import com.blinkbox.books.json
import org.json4s._
import org.json4s.ext.EnumNameSerializer
import spray.httpx.Json4sJacksonSupport

import scala.reflect.ClassTag

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

  class EnumSerializer[T: ClassTag](container: EnumContainer[T]) extends Serializer[T] {
    val cl = implicitly[ClassTag[T]].runtimeClass

    override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), T] = {
      case (TypeInfo(c, _), JString(v)) if c == cl => container.fromString(v)
    }

    override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case v: T => JString(container.toString(v))
    }
  }

  implicit val json4sJacksonFormats: Formats = json.DefaultFormats + new EnumSerializer(SsoTokenElevation) + new EnumSerializer(SsoTokenStatus) +
    new EnumNameSerializer(Elevation) + snakeizer
}

