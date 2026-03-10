package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.google.protobuf.struct.{Struct, Value, ListValue, NullValue}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scalapb_json.ProtoMacrosCommon.{protoStructToExpr, protoValueToExpr}
import scala.quoted.*
import scala.reflect.NameTransformer.MODULE_INSTANCE_NAME
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

object ProtoMacrosJsoniter {

  private val structCodec: JsonValueCodec[Struct] = new JsonValueCodec[Struct] {
    def nullValue: Struct = Struct()
    def decodeValue(in: JsonReader, default: Struct): Struct = StructFormat.structReader(in)
    def encodeValue(x: Struct, out: JsonWriter): Unit = StructFormat.structWriter(x, out)
  }

  private val valueCodec: JsonValueCodec[Value] = new JsonValueCodec[Value] {
    def nullValue: Value = Value()
    def decodeValue(in: JsonReader, default: Value): Value = StructFormat.structValueReader(in)
    def encodeValue(x: Value, out: JsonWriter): Unit = StructFormat.structValueWriter(x, out)
  }

  extension (inline s: StringContext) {
    inline def struct(): Struct =
      ${ structInterpolation('s) }
    inline def value(): Value =
      ${ valueInterpolation('s) }
  }

  extension [A <: GeneratedMessage](inline companion: GeneratedMessageCompanion[A]) {
    inline def fromJsonConstant(inline json: String): A =
      ${ fromJsonConstantImpl[A]('json, 'companion) }
  }

  extension [A <: GeneratedMessage](companion: GeneratedMessageCompanion[A]) {
    def fromJson(json: String): A =
      JsonFormat.fromJsonString[A](json)(using companion)

    def fromJsonOpt(json: String): Option[A] =
      try {
        Some(fromJson(json))
      } catch {
        case NonFatal(_) =>
          None
      }

    def fromJsonEither(json: String): Either[Throwable, A] =
      try {
        Right(fromJson(json))
      } catch {
        case NonFatal(e) =>
          Left(e)
      }

    def fromJsonTry(json: String): Try[A] =
      try {
        Success(fromJson(json))
      } catch {
        case NonFatal(e) =>
          Failure(e)
      }
  }

  private def structInterpolation(s: Expr[StringContext])(using quote: Quotes): Expr[Struct] = {
    import quote.reflect.report
    given ToExpr[Struct] = protoStructToExpr
    val Seq(str) = s.valueOrAbort.parts
    try {
      val result = readFromString[Struct](str)(structCodec)
      Expr(result)
    } catch {
      case e: Exception => report.errorAndAbort(e.getMessage)
    }
  }

  private def valueInterpolation(s: Expr[StringContext])(using quote: Quotes): Expr[Value] = {
    import quote.reflect.report
    given ToExpr[Value] = protoValueToExpr
    val Seq(str) = s.valueOrAbort.parts
    try {
      val result = readFromString[Value](str)(valueCodec)
      Expr(result)
    } catch {
      case e: Exception => report.errorAndAbort(e.getMessage)
    }
  }

  private def fromJsonConstantImpl[A <: GeneratedMessage: Type](
    json: Expr[String],
    companion: Expr[GeneratedMessageCompanion[A]]
  )(using quote: Quotes): Expr[A] = {
    import quote.reflect.report
    val str = json.valueOrAbort
    val clazz = Class.forName(Type.show[A] + "$")
    val c: GeneratedMessageCompanion[A] =
      clazz.getField(MODULE_INSTANCE_NAME).get(null).asInstanceOf[GeneratedMessageCompanion[A]]

    // Validate at compile time
    try {
      JsonFormat.fromJsonString[A](str)(using c)
    } catch {
      case e: Exception => report.errorAndAbort(e.getMessage)
    }

    '{
      JsonFormat.fromJsonString[A]($json)(using $companion)
    }
  }
}
