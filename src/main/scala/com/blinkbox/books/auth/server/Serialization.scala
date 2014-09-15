package com.blinkbox.books.auth.server

import com.blinkbox.books.auth.{UserRole, Elevation}
import com.blinkbox.books.auth.UserRole.UserRole
import com.blinkbox.books.json.DefaultFormats
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.ext.EnumNameSerializer
import spray.http._
import spray.httpx.Json4sJacksonSupport
import spray.httpx.marshalling.Marshaller

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

trait FormUnicodeSupport {
  implicit val FormDataMarshaller: Marshaller[FormData] =
    Marshaller.delegate[FormData, String](MediaTypes.`application/x-www-form-urlencoded`) { (formData, contentType) ⇒
      Uri.Query(formData.fields: _*).render(new StringRendering, HttpCharsets.`UTF-8`.nioCharset).get
    }
}

class Serialization extends Json4sJacksonSupport {
  implicit def json4sJacksonFormats: Formats = DefaultFormats + ZuulRequestExceptionSerializer +
    new EnumNameSerializer(TokenStatus) + new EnumNameSerializer(Elevation) + new EnumNameSerializer(UserRole)
}

object Serialization extends Serialization
