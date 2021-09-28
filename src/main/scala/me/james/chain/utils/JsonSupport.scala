package me.james.chain.utils

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.StatusCodes.ClientError
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.Timeout
import io.circe.jawn.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, Printer}

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.duration.DurationInt

trait CirceEncoders {
  implicit val timeout: Timeout         = Timeout(20.seconds)
  val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  implicit object DateTimeEncoder extends Encoder[OffsetDateTime] {
    override def apply(dt: OffsetDateTime): Json = dateTimeFormat.format(dt).asJson
  }

  implicit object UuidEncoder extends Encoder[UUID] {
    override def apply(u: UUID): Json = u.toString.asJson
  }

  implicit object ClientErrorEncoder extends Encoder[ClientError] {
    override def apply(a: ClientError): Json = a.defaultMessage.asJson
  }
}

trait JsonSupport extends CirceEncoders {

  //  implicit def materializer: Materializer

  val noSpacesDropNull: Printer = Printer.noSpaces.copy(dropNullValues = true)

  implicit def circeUnmarshaller[A <: Product: Manifest](implicit d: Decoder[A]): FromEntityUnmarshaller[A] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input =
          if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        decode[A](input) match {
          case Right(obj)    => obj
          case Left(failure) => throw new IllegalArgumentException(failure.getMessage, failure.getCause)
        }
      }

  implicit def circeMarshaller[A <: AnyRef](implicit e: Encoder[A], cbs: CanBeSerialized[A]): ToEntityMarshaller[A] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`) {
      e(_).noSpaces
    }

  def statusWithdata(code: StatusCode, data: String = "") = {
    def strict: HttpEntity.Strict =
      HttpEntity(ContentTypes.`application/json`, s"""{"code":${code.intValue()},"data":$data}""")
    complete(code, strict)
  }

  /** To limit what data can be serialized to the client, only classes of type `T` for which an implicit
    * `CanBeSerialized[T]` value is in scope will be allowed. You only need to provide an implicit for the base value,
    * any containers like `List` or `Option` will be automatically supported.
    */
  trait CanBeSerialized[T]

  object CanBeSerialized {
    def apply[T]                                                                                        = new CanBeSerialized[T] {}
    implicit def listCanBeSerialized[T](implicit cbs: CanBeSerialized[T]): CanBeSerialized[List[T]]     = null
    implicit def setCanBeSerialized[T](implicit cbs: CanBeSerialized[T]): CanBeSerialized[Set[T]]       = null
    implicit def optionCanBeSerialized[T](implicit cbs: CanBeSerialized[T]): CanBeSerialized[Option[T]] = null
  }
}
