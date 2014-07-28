package com.blinkbox.books.auth.server

import org.joda.time.DateTime
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s.ext.{JodaTimeSerializers, EnumNameSerializer}
import org.json4s._
import JsonDSL._
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

object Serialization extends Json4sJacksonSupport {
  val clientInfoSerializer = FieldSerializer[ClientInfo](
    serializer = {
      case ("last_used_date", d) => Some("last_used_date", d.asInstanceOf[DateTime].toString("yyyy-MM-dd"))
    }
  )

  implicit def json4sJacksonFormats: Formats = (DefaultFormats + ZuulRequestExceptionSerializer +
    new EnumNameSerializer(RefreshTokenStatus) + clientInfoSerializer) ++ JodaTimeSerializers.all
}
