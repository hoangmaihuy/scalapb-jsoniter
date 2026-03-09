package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.google.protobuf.ByteString
import com.google.protobuf.descriptor.FieldDescriptorProto
import com.google.protobuf.duration.Duration
import com.google.protobuf.field_mask.FieldMask
import com.google.protobuf.struct.NullValue
import com.google.protobuf.timestamp.Timestamp
import scalapb.*
import scalapb.descriptors.*
import scalapb_json.*
import scalapb_json.ScalapbJsonCommon.GenericCompanion

import scala.util.control.NonFatal

class Printer(
  val includingDefaultValueFields: Boolean = false,
  val preservingProtoFieldNames: Boolean = false,
  val formattingLongAsNumber: Boolean = false,
  val formattingEnumsAsNumber: Boolean = false,
  val formattingMapEntriesAsKeyValuePairs: Boolean = false,
  val typeRegistry: TypeRegistry = TypeRegistry.empty
) {

  def print[A <: GeneratedMessage](m: A): String = {
    val codec = new JsonValueCodec[A] {
      def nullValue: A = null.asInstanceOf[A]
      def encodeValue(x: A, out: JsonWriter): Unit = toJson(x, out)
      def decodeValue(in: JsonReader, default: A): A = throw new UnsupportedOperationException
    }
    writeToString(m)(codec)
  }

  def toJson[A <: GeneratedMessage](m: A, out: JsonWriter): Unit = {
    // Check for well-known types
    m match {
      case d: Duration =>
        out.writeVal(Durations.writeDuration(d))
      case t: Timestamp =>
        out.writeVal(Timestamps.writeTimestamp(t))
      case f: FieldMask =>
        out.writeVal(ScalapbJsonCommon.fieldMaskToJsonString(f))
      case v: com.google.protobuf.struct.Value =>
        StructFormat.structValueWriter(v, out)
      case s: com.google.protobuf.struct.Struct =>
        StructFormat.structWriter(s, out)
      case l: com.google.protobuf.struct.ListValue =>
        StructFormat.listValueWriter(l, out)
      case a: com.google.protobuf.any.Any =>
        AnyFormat.anyWriter(this, a, out)
      case _ if isWrapperType(m) =>
        writePrimitiveWrapper(m, out)
      case _ =>
        writeMessage(m, out)
    }
  }

  private def isWrapperType(m: GeneratedMessage): Boolean = {
    m match {
      case _: com.google.protobuf.wrappers.DoubleValue |
           _: com.google.protobuf.wrappers.FloatValue |
           _: com.google.protobuf.wrappers.Int32Value |
           _: com.google.protobuf.wrappers.Int64Value |
           _: com.google.protobuf.wrappers.UInt32Value |
           _: com.google.protobuf.wrappers.UInt64Value |
           _: com.google.protobuf.wrappers.BoolValue |
           _: com.google.protobuf.wrappers.BytesValue |
           _: com.google.protobuf.wrappers.StringValue => true
      case _ => false
    }
  }

  private def writePrimitiveWrapper(m: GeneratedMessage, out: JsonWriter): Unit = {
    val fieldDesc = m.companion.scalaDescriptor.findFieldByNumber(1).get
    serializeSingleValue(fieldDesc, m.getField(fieldDesc), out)
  }

  private def writeMessage[A <: GeneratedMessage](m: A, out: JsonWriter): Unit = {
    out.writeObjectStart()
    val descriptor = m.companion.scalaDescriptor
    descriptor.fields.foreach { f =>
      val name = if (preservingProtoFieldNames) f.name else ScalapbJsonCommon.jsonName(f)
      if (f.protoType.isTypeMessage) {
        serializeMessageField(f, name, m.getFieldByNumber(f.number), out)
      } else {
        serializeNonMessageField(f, name, m.getField(f), out)
      }
    }
    out.writeObjectEnd()
  }

  private[scalapb_jsoniter] def serializeMessageField(
    fd: FieldDescriptor,
    name: String,
    value: Any,
    out: JsonWriter
  ): Unit = {
    value match {
      case null =>
      case Nil =>
        if (includingDefaultValueFields) {
          out.writeKey(name)
          if (fd.isMapField && !formattingMapEntriesAsKeyValuePairs) {
            out.writeObjectStart(); out.writeObjectEnd()
          } else {
            out.writeArrayStart(); out.writeArrayEnd()
          }
        }
      case xs: Iterable[GeneratedMessage] @unchecked =>
        if (fd.isMapField && !formattingMapEntriesAsKeyValuePairs) {
          val mapEntryDescriptor = fd.scalaType.asInstanceOf[ScalaType.Message].descriptor
          val keyDescriptor = mapEntryDescriptor.findFieldByNumber(1).get
          val valueDescriptor = mapEntryDescriptor.findFieldByNumber(2).get
          out.writeKey(name)
          out.writeObjectStart()
          xs.foreach { x =>
            val key = x.getField(keyDescriptor) match {
              case PBoolean(v) => v.toString
              case PDouble(v)  => v.toString
              case PFloat(v)   => v.toString
              case PInt(v)     => v.toString
              case PLong(v)    => v.toString
              case PString(v)  => v
              case v => throw new JsonFormatException(s"Unexpected value for key: $v")
            }
            out.writeKey(key)
            if (valueDescriptor.protoType.isTypeMessage) {
              toJson(x.getFieldByNumber(valueDescriptor.number).asInstanceOf[GeneratedMessage], out)
            } else {
              serializeSingleValue(valueDescriptor, x.getField(valueDescriptor), out)
            }
          }
          out.writeObjectEnd()
        } else {
          out.writeKey(name)
          out.writeArrayStart()
          xs.foreach(toJson(_, out))
          out.writeArrayEnd()
        }
      case msg: GeneratedMessage =>
        out.writeKey(name)
        toJson(msg, out)
      case v =>
        throw new JsonFormatException(v.toString)
    }
  }

  private[scalapb_jsoniter] def serializeNonMessageField(
    fd: FieldDescriptor,
    name: String,
    value: PValue,
    out: JsonWriter
  ): Unit = {
    value match {
      case PEmpty =>
        if (includingDefaultValueFields && fd.containingOneof.isEmpty) {
          out.writeKey(name)
          writeDefaultValue(fd, out)
        }
      case PRepeated(xs) =>
        if (xs.nonEmpty || includingDefaultValueFields) {
          out.writeKey(name)
          out.writeArrayStart()
          xs.foreach(serializeSingleValue(fd, _, out))
          out.writeArrayEnd()
        }
      case v =>
        if (
          includingDefaultValueFields ||
          !fd.isOptional ||
          !fd.file.isProto3 ||
          (v != ScalapbJsonCommon.defaultValue(fd)) ||
          fd.containingOneof.isDefined
        ) {
          out.writeKey(name)
          serializeSingleValue(fd, v, out)
        }
    }
  }

  private def writeDefaultValue(fd: FieldDescriptor, out: JsonWriter): Unit = {
    serializeSingleValue(fd, ScalapbJsonCommon.defaultValue(fd), out)
  }

  private def unsignedLong(n: Long): BigDecimal =
    if (n < 0) BigDecimal(BigInt(n & 0x7fffffffffffffffL).setBit(63)) else BigDecimal(n)

  private def formatLong(n: Long, protoType: FieldDescriptorProto.Type, out: JsonWriter): Unit = {
    val v = if (protoType.isTypeUint64 || protoType.isTypeFixed64) unsignedLong(n) else BigDecimal(n)
    if (formattingLongAsNumber) out.writeVal(v)
    else out.writeVal(v.toString())
  }

  private[scalapb_jsoniter] def serializeEnum(e: EnumValueDescriptor, out: JsonWriter): Unit = {
    if (formattingEnumsAsNumber) out.writeVal(e.number)
    else out.writeVal(e.name)
  }

  def serializeSingleValue(fd: FieldDescriptor, value: PValue, out: JsonWriter): Unit = {
    value match {
      case PEnum(e) =>
        if (e.containingEnum == NullValue.scalaDescriptor) {
          out.writeNull()
        } else {
          serializeEnum(e, out)
        }
      case PInt(v) if fd.protoType.isTypeUint32 || fd.protoType.isTypeFixed32 =>
        out.writeVal(ScalapbJsonCommon.unsignedInt(v))
      case PInt(v) =>
        out.writeVal(v)
      case PLong(v) =>
        formatLong(v, fd.protoType, out)
      case PDouble(v) =>
        if (v.isNaN) out.writeVal("NaN")
        else if (v.isPosInfinity) out.writeVal("Infinity")
        else if (v.isNegInfinity) out.writeVal("-Infinity")
        else out.writeVal(v)
      case PFloat(v) =>
        if (v.isNaN) out.writeVal("NaN")
        else if (v.isPosInfinity) out.writeVal("Infinity")
        else if (v.isNegInfinity) out.writeVal("-Infinity")
        else out.writeVal(v)
      case PBoolean(v) =>
        out.writeVal(v)
      case PString(v) =>
        out.writeVal(v)
      case PByteString(v) =>
        out.writeVal(java.util.Base64.getEncoder.encodeToString(v.toByteArray()))
      case _: PMessage | PRepeated(_) | PEmpty =>
        throw new RuntimeException("Should not happen")
    }
  }
}

class Parser(
  val preservingProtoFieldNames: Boolean = false,
  val mapEntriesAsKeyValuePairs: Boolean = false,
  val typeRegistry: TypeRegistry = TypeRegistry.empty
) {

  def fromJsonString[A <: GeneratedMessage](str: String)(implicit cmp: GeneratedMessageCompanion[A]): A = {
    val self = this
    val codec = new JsonValueCodec[A] {
      def nullValue: A = null.asInstanceOf[A]
      def encodeValue(x: A, out: JsonWriter): Unit = throw new UnsupportedOperationException
      def decodeValue(in: JsonReader, default: A): A = self.fromJson[A](in)
    }
    readFromString(str)(codec)
  }

  def fromJson[A <: GeneratedMessage](in: JsonReader)(implicit cmp: GeneratedMessageCompanion[A]): A = {
    cmp.messageReads.read(fromJsonToPMessage(cmp, in))
  }

  private def serializedName(fd: FieldDescriptor): String = {
    if (preservingProtoFieldNames) fd.asProto.getName else ScalapbJsonCommon.jsonName(fd)
  }

  private def fromJsonToPMessage(cmp: GeneratedMessageCompanion[?], in: JsonReader): PMessage = {
    val klass = ScalapbJsoniterCompat.getClassFromMessageCompanion(cmp)

    // Check well-known types
    if (classOf[Duration].isAssignableFrom(klass)) {
      return Durations.parseDuration(in.readString(null)).toPMessage
    }
    if (classOf[Timestamp].isAssignableFrom(klass)) {
      return Timestamps.parseTimestamp(in.readString(null)).toPMessage
    }
    if (classOf[FieldMask].isAssignableFrom(klass)) {
      return ScalapbJsonCommon.fieldMaskFromJsonString(in.readString(null)).toPMessage
    }
    if (classOf[com.google.protobuf.struct.Value].isAssignableFrom(klass)) {
      return StructFormat.structValueReader(in).toPMessage
    }
    if (classOf[com.google.protobuf.struct.Struct].isAssignableFrom(klass)) {
      return StructFormat.structReader(in).toPMessage
    }
    if (classOf[com.google.protobuf.struct.ListValue].isAssignableFrom(klass)) {
      return StructFormat.listValueReader(in).toPMessage
    }
    if (classOf[com.google.protobuf.any.Any].isAssignableFrom(klass)) {
      return AnyFormat.anyReader(this, in).toPMessage
    }
    if (isWrapperClass(klass)) {
      return parsePrimitiveWrapper(cmp, in)
    }

    parseObject(cmp, in)
  }

  private def isWrapperClass(klass: Class[?]): Boolean = {
    classOf[com.google.protobuf.wrappers.DoubleValue].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.FloatValue].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.Int32Value].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.Int64Value].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.UInt32Value].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.UInt64Value].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.BoolValue].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.BytesValue].isAssignableFrom(klass) ||
    classOf[com.google.protobuf.wrappers.StringValue].isAssignableFrom(klass)
  }

  private def parsePrimitiveWrapper(cmp: GeneratedMessageCompanion[?], in: JsonReader): PMessage = {
    val fieldDesc = cmp.scalaDescriptor.findFieldByNumber(1).get
    val value = parsePrimitive(fieldDesc.scalaType, fieldDesc.protoType, in)
    PMessage(Map(fieldDesc -> value))
  }

  /** Try to read a JSON null value. For repeated/map fields, null at the top level
    * means "skip/use default". For other fields, null also means "skip/use default".
    * Returns true if null was consumed.
    */
  private def tryReadNull(in: JsonReader, fd: FieldDescriptor): Boolean = {
    // Use skip approach: peek at next token, if it looks like null, read raw and check
    val isNull = in.isNextToken('n')
    if (isNull) {
      // We saw 'n' — this should be null. Read remaining 'ull' via skip
      in.rollbackToken()
      in.skip() // skips the entire null token
      true
    } else {
      in.rollbackToken()
      false
    }
  }

  private def parseObject(cmp: GeneratedMessageCompanion[?], in: JsonReader): PMessage = {
    if (!in.isNextToken('{')) {
      throw new JsonFormatException(s"Expected an object")
    }
    if (in.isNextToken('}')) {
      return PMessage(Map.empty)
    }
    in.rollbackToken()

    val fieldsByName: Map[String, FieldDescriptor] =
      cmp.scalaDescriptor.fields.map(f => serializedName(f) -> f).toMap

    val valueMap = scala.collection.mutable.Map.empty[FieldDescriptor, PValue]
    while ({
      val key = in.readKeyAsString()
      fieldsByName.get(key) match {
        case Some(fd) =>
          if (tryReadNull(in, fd)) {
            // null values are skipped (give default)
          } else {
            valueMap += (fd -> parseValue(cmp, fd, in))
          }
        case None =>
          // Unknown field, skip
          in.skip()
      }
      in.isNextToken(',')
    }) ()

    PMessage(valueMap.toMap)
  }

  private def parseValue(
    cmp: GeneratedMessageCompanion[?],
    fd: FieldDescriptor,
    in: JsonReader
  ): PValue = {
    if (fd.isMapField && !mapEntriesAsKeyValuePairs) {
      parseMapField(cmp, fd, in)
    } else if (fd.isRepeated) {
      parseRepeatedField(cmp, fd, in)
    } else {
      parseSingleValue(cmp, fd, in)
    }
  }

  private def parseMapField(
    cmp: GeneratedMessageCompanion[?],
    fd: FieldDescriptor,
    in: JsonReader
  ): PValue = {
    if (!in.isNextToken('{')) {
      throw new JsonFormatException(
        s"Expected an object for map field ${serializedName(fd)} of ${fd.containingMessage.name}"
      )
    }
    if (in.isNextToken('}')) {
      return PRepeated(Vector.empty)
    }
    in.rollbackToken()

    val mapEntryDesc = fd.scalaType.asInstanceOf[ScalaType.Message].descriptor
    val keyDescriptor = mapEntryDesc.findFieldByNumber(1).get
    val valueDescriptor = mapEntryDesc.findFieldByNumber(2).get

    val entries = scala.collection.mutable.ArrayBuffer.empty[PValue]
    while ({
      val key = in.readKeyAsString()
      val keyObj = keyDescriptor.scalaType match {
        case ScalaType.Boolean => PBoolean(java.lang.Boolean.valueOf(key))
        case ScalaType.Double  => PDouble(java.lang.Double.valueOf(key))
        case ScalaType.Float   => PFloat(java.lang.Float.valueOf(key))
        case ScalaType.Int     => PInt(java.lang.Integer.valueOf(key))
        case ScalaType.Long    => PLong(java.lang.Long.valueOf(key))
        case ScalaType.String  => PString(key)
        case _                 => throw new RuntimeException(s"Unsupported type for key for ${fd.name}")
      }
      val mapEntryCmp = cmp.messageCompanionForFieldNumber(fd.number)
      val valueObj = if (valueDescriptor.protoType.isTypeMessage) {
        fromJsonToPMessage(
          mapEntryCmp.messageCompanionForFieldNumber(valueDescriptor.number),
          in
        )
      } else {
        parseSingleValue(mapEntryCmp, valueDescriptor, in)
      }
      entries += PMessage(Map(keyDescriptor -> keyObj, valueDescriptor -> valueObj))
      in.isNextToken(',')
    }) ()
    PRepeated(entries.toVector)
  }

  private def parseRepeatedField(
    cmp: GeneratedMessageCompanion[?],
    fd: FieldDescriptor,
    in: JsonReader
  ): PValue = {
    if (!in.isNextToken('[')) {
      throw new JsonFormatException(
        s"Expected an array for repeated field ${serializedName(fd)} of ${fd.containingMessage.name}"
      )
    }
    if (in.isNextToken(']')) {
      return PRepeated(Vector.empty)
    }
    in.rollbackToken()
    val values = scala.collection.mutable.ArrayBuffer.empty[PValue]
    while ({
      values += parseSingleValue(cmp, fd, in)
      in.isNextToken(',')
    }) ()
    PRepeated(values.toVector)
  }

  private def parseSingleValue(
    containerCompanion: GeneratedMessageCompanion[?],
    fd: FieldDescriptor,
    in: JsonReader
  ): PValue = {
    fd.scalaType match {
      case ScalaType.Enum(ed) =>
        parseEnum(ed, in)
      case ScalaType.Message(_) =>
        fromJsonToPMessage(containerCompanion.messageCompanionForFieldNumber(fd.number), in)
      case st =>
        parsePrimitive(st, fd.protoType, in)
    }
  }

  private def parseEnum(ed: EnumDescriptor, in: JsonReader): PValue = {
    // Enums can be represented as string or number
    val b = in.nextToken()
    in.rollbackToken()
    if (b == '"') {
      val s = in.readString(null)
      val evd = ed.values.find(_.name == s).getOrElse(
        throw new JsonFormatException(s"Unrecognized enum value '$s'")
      )
      PEnum(evd)
    } else if (b == 'n') {
      // NullValue special case — skip the null token
      in.skip()
      if (ed == NullValue.scalaDescriptor) {
        PEnum(NullValue.NULL_VALUE.scalaValueDescriptor)
      } else {
        throw new JsonFormatException(s"Unexpected null for enum ${ed.fullName}")
      }
    } else {
      val n = in.readInt()
      val evd = ed.findValueByNumber(n).getOrElse(
        throw new JsonFormatException(s"Invalid enum value: $n for enum type: ${ed.fullName}")
      )
      PEnum(evd)
    }
  }

  private[scalapb_jsoniter] def parsePrimitive(
    scalaType: ScalaType,
    protoType: FieldDescriptorProto.Type,
    in: JsonReader
  ): PValue = {
    scalaType match {
      case ScalaType.Int =>
        val b = in.nextToken()
        in.rollbackToken()
        if (b == '"') {
          val s = in.readString(null)
          if (protoType.isTypeInt32 || protoType.isTypeSint32) {
            ScalapbJsonCommon.parseInt32(s)
          } else {
            ScalapbJsonCommon.parseUint32(s)
          }
        } else {
          PInt(in.readInt())
        }
      case ScalaType.Long =>
        val b = in.nextToken()
        in.rollbackToken()
        if (b == '"') {
          val s = in.readString(null)
          if (protoType.isTypeInt64 || protoType.isTypeSint64) {
            ScalapbJsonCommon.parseInt64(s)
          } else {
            ScalapbJsonCommon.parseUint64(s)
          }
        } else {
          PLong(in.readLong())
        }
      case ScalaType.Double =>
        val b = in.nextToken()
        in.rollbackToken()
        if (b == '"') {
          in.readString(null) match {
            case "NaN"       => PDouble(Double.NaN)
            case "Infinity"  => PDouble(Double.PositiveInfinity)
            case "-Infinity" => PDouble(Double.NegativeInfinity)
            case s => throw new JsonFormatException(s"Unexpected string value for double: $s")
          }
        } else {
          PDouble(in.readDouble())
        }
      case ScalaType.Float =>
        val b = in.nextToken()
        in.rollbackToken()
        if (b == '"') {
          in.readString(null) match {
            case "NaN"       => PFloat(Float.NaN)
            case "Infinity"  => PFloat(Float.PositiveInfinity)
            case "-Infinity" => PFloat(Float.NegativeInfinity)
            case s => throw new JsonFormatException(s"Unexpected string value for float: $s")
          }
        } else {
          PFloat(in.readFloat())
        }
      case ScalaType.Boolean =>
        val b = in.nextToken()
        in.rollbackToken()
        if (b == '"') {
          in.readString(null) match {
            case "true"  => PBoolean(true)
            case "false" => PBoolean(false)
            case s => throw new JsonFormatException(s"Unexpected string value for boolean: $s")
          }
        } else {
          PBoolean(in.readBoolean())
        }
      case ScalaType.String =>
        PString(in.readString(null))
      case ScalaType.ByteString =>
        val s = in.readString(null)
        PByteString(ByteString.copyFrom(java.util.Base64.getDecoder.decode(s)))
      case _ =>
        throw new JsonFormatException(s"Unsupported type: $scalaType")
    }
  }
}

object JsonFormat {
  val printer = new Printer()
  val parser = new Parser()

  def toJsonString[A <: GeneratedMessage](m: A): String = printer.print(m)

  def fromJsonString[A <: GeneratedMessage: GeneratedMessageCompanion](str: String): A = {
    parser.fromJsonString(str)
  }

}
