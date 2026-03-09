package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.google.protobuf.any.{Any => PBAny}
import scalapb_json.*

object AnyFormat {

  def anyWriter(printer: Printer, any: PBAny, out: JsonWriter): Unit = {
    val cmp = printer.typeRegistry
      .findType(any.typeUrl)
      .getOrElse(
        throw new IllegalStateException(
          s"Unknown type ${any.typeUrl}; you may have to register it via TypeRegistry"
        )
      )
    val message = any.unpack(cmp)
    out.writeObjectStart()
    out.writeKey("@type")
    out.writeVal(any.typeUrl)
    val descriptor = message.companion.scalaDescriptor
    descriptor.fields.foreach { f =>
      val name = scalapb_json.ScalapbJsonCommon.jsonName(f)
      if (f.protoType.isTypeMessage) {
        printer.serializeMessageField(f, name, message.getFieldByNumber(f.number), out)
      } else {
        printer.serializeNonMessageField(f, name, message.getField(f), out)
      }
    }
    out.writeObjectEnd()
  }

  // A codec that reads Any JSON into a simple (typeUrl, rawBytes) pair
  // by first finding @type, then re-parsing the whole thing as the target message
  def anyReader(parser: Parser, in: JsonReader): PBAny = {
    // Read raw JSON bytes so we can parse it twice
    val raw = in.readRawValAsBytes()
    // First pass: extract @type from the raw JSON
    val typeUrl = extractTypeUrl(raw)

    val cmp = parser.typeRegistry
      .findType(typeUrl)
      .getOrElse(throw new JsonFormatException(s"""Unknown type: "$typeUrl""""))

    // Second pass: parse the message from the raw JSON
    // Use readFromArrayReentrant to avoid corrupting the outer pooled reader's state
    val msgCodec = new JsonValueCodec[scalapb.GeneratedMessage] {
      def nullValue: scalapb.GeneratedMessage = null
      def encodeValue(x: scalapb.GeneratedMessage, out: JsonWriter): Unit =
        throw new UnsupportedOperationException
      def decodeValue(in: JsonReader, default: scalapb.GeneratedMessage): scalapb.GeneratedMessage =
        parser.fromJson(in)(cmp)
    }
    val message = readFromArrayReentrant(raw)(msgCodec)
    PBAny(typeUrl = typeUrl, value = message.toByteString)
  }

  private def extractTypeUrl(raw: Array[Byte]): String = {
    var typeUrl: String = null
    val extractor = new JsonValueCodec[Unit] {
      def nullValue: Unit = ()
      def encodeValue(x: Unit, out: JsonWriter): Unit =
        throw new UnsupportedOperationException
      def decodeValue(in: JsonReader, default: Unit): Unit = {
        if (!in.isNextToken('{')) {
          throw new JsonFormatException("Expected an object for Any")
        }
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          while ({
            val key = in.readKeyAsString()
            if (key == "@type") {
              typeUrl = in.readString(null)
            } else {
              in.skip()
            }
            in.isNextToken(',')
          }) ()
        }
      }
    }
    readFromArrayReentrant(raw)(extractor)
    if (typeUrl == null) {
      throw new JsonFormatException("Expected string @type field in Any")
    }
    typeUrl
  }
}
