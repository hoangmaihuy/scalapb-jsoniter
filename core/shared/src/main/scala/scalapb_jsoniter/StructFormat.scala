package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.google.protobuf.struct
import com.google.protobuf.struct.Value.Kind
import scalapb_json.*

object StructFormat {

  def structValueWriter(v: struct.Value, out: JsonWriter): Unit = {
    v.kind match {
      case Kind.Empty            => out.writeNull()
      case Kind.NullValue(_)     => out.writeNull()
      case Kind.NumberValue(value) =>
        val l = value.toLong
        if (l.toDouble == value) out.writeVal(l)
        else out.writeVal(value)
      case Kind.StringValue(value) => out.writeVal(value)
      case Kind.BoolValue(value)   => out.writeVal(value)
      case Kind.StructValue(value) => structWriter(value, out)
      case Kind.ListValue(value)   => listValueWriter(value, out)
    }
  }

  def structValueReader(in: JsonReader): struct.Value = {
    val b = in.nextToken()
    in.rollbackToken()
    val kind: struct.Value.Kind = b match {
      case 'n' =>
        in.skip()
        Kind.NullValue(struct.NullValue.NULL_VALUE)
      case '"' =>
        Kind.StringValue(in.readString(null))
      case 't' | 'f' =>
        Kind.BoolValue(in.readBoolean())
      case '{' =>
        Kind.StructValue(structReader(in))
      case '[' =>
        Kind.ListValue(listValueReader(in))
      case _ =>
        Kind.NumberValue(in.readDouble())
    }
    struct.Value(kind = kind)
  }

  def structWriter(v: struct.Struct, out: JsonWriter): Unit = {
    out.writeObjectStart()
    v.fields.foreach { case (key, value) =>
      out.writeKey(key)
      structValueWriter(value, out)
    }
    out.writeObjectEnd()
  }

  def structReader(in: JsonReader): struct.Struct = {
    if (!in.isNextToken('{')) {
      throw new JsonFormatException("Expected an object")
    } else if (in.isNextToken('}')) {
      struct.Struct()
    } else {
      in.rollbackToken()
      val fields = scala.collection.mutable.Map.empty[String, struct.Value]
      while ({
        val key = in.readKeyAsString()
        fields += (key -> structValueReader(in))
        in.isNextToken(',')
      }) ()
      struct.Struct(fields = fields.toMap)
    }
  }

  def listValueWriter(v: struct.ListValue, out: JsonWriter): Unit = {
    out.writeArrayStart()
    v.values.foreach(structValueWriter(_, out))
    out.writeArrayEnd()
  }

  def listValueReader(in: JsonReader): struct.ListValue = {
    if (!in.isNextToken('[')) {
      throw new JsonFormatException("Expected an array")
    } else if (in.isNextToken(']')) {
      struct.ListValue()
    } else {
      in.rollbackToken()
      val values = scala.collection.mutable.ArrayBuffer.empty[struct.Value]
      while ({
        values += structValueReader(in)
        in.isNextToken(',')
      }) ()
      struct.ListValue(values.toSeq)
    }
  }
}
