package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.Elevation
import com.blinkbox.books.json.DefaultFormats
import org.joda.time.DateTime
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.ext.EnumNameSerializer
import spray.httpx.Json4sJacksonSupport

object ZuulRequestExceptionSerializer extends Serializer[ZuulRequestException] {
  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), ZuulRequestException] = {

    case (_, JObject(
    JField("error", JString(code)) :: JField("error_reason", JString(reason)) :: JField("error_description", JString(message)) :: Nil
    )) => ZuulRequestException(message, ZuulRequestErrorCode.fromString(code), Some(ZuulRequestErrorReason.fromString(reason)))

    case (_, JObject(
    JField("error", JString(code)) :: JField("error_description", JString(message)) :: Nil
    )) => ZuulRequestException(message, ZuulRequestErrorCode.fromString(code), None)
  }

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case e: ZuulRequestException =>
      ("error" -> ZuulRequestErrorCode.toString(e.code)) ~
        ("error_reason" -> e.reason.map(ZuulRequestErrorReason.toString)) ~
        ("error_description" -> e.message)
  }
}

class Serialization extends Json4sJacksonSupport {
  val clientInfoSerializer = FieldSerializer[ClientInfo](
    serializer = {
      case ("last_used_date", d) => Some("last_used_date", d.asInstanceOf[DateTime].toString("yyyy-MM-dd"))
    }
  )

  implicit def json4sJacksonFormats: Formats = DefaultFormats + ZuulRequestExceptionSerializer +
    new EnumNameSerializer(RefreshTokenStatus) + new EnumNameSerializer(Elevation) + clientInfoSerializer
}

object Serialization extends Serialization
