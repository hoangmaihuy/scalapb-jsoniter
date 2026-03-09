package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import scalapb.{GeneratedEnum, GeneratedEnumCompanion, GeneratedMessage, GeneratedMessageCompanion}
import scalapb_json.JsonFormatException

object codec {

  implicit def printer: Printer = JsonFormat.printer
  implicit def parser: Parser = JsonFormat.parser

  implicit def generatedMessageCodec[M <: GeneratedMessage: GeneratedMessageCompanion](implicit
    p: Printer,
    pa: Parser
  ): JsonValueCodec[M] = new JsonValueCodec[M] {
    def nullValue: M = null.asInstanceOf[M]
    def encodeValue(x: M, out: JsonWriter): Unit = p.toJson(x, out)
    def decodeValue(in: JsonReader, default: M): M = pa.fromJson[M](in)
  }

  implicit def generatedEnumCodec[E <: GeneratedEnum: GeneratedEnumCompanion](implicit
    p: Printer,
    pa: Parser
  ): JsonValueCodec[E] = new JsonValueCodec[E] {
    private val companion = implicitly[GeneratedEnumCompanion[E]]

    def nullValue: E = null.asInstanceOf[E]

    def encodeValue(x: E, out: JsonWriter): Unit = {
      p.serializeEnum(x.scalaValueDescriptor, out)
    }

    def decodeValue(in: JsonReader, default: E): E = {
      val b = in.nextToken()
      in.rollbackToken()
      if (b == '"') {
        val s = in.readString(null)
        val evd = companion.scalaDescriptor.values.find(_.name == s).getOrElse(
          throw new JsonFormatException(s"Unrecognized enum value '$s'")
        )
        companion.fromValue(evd.number)
      } else {
        val n = in.readInt()
        companion.fromValue(n)
      }
    }
  }
}
