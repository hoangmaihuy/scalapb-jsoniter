package scalapb_jsoniter

import com.google.protobuf.struct.*
import jsontest.test3.StructTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class StructFormatSpec extends AnyFlatSpec with Matchers with JavaAssertions {
  "Empty value" should "be serialized to null" in {
    JsonFormat.toJsonString(Value()) must be("null")
  }

  "Struct with null in array" should "parse correctly" in {
    val json = """{ "a" : [1, false, null, "x"] }"""
    val result = JsonFormat.fromJsonString[Struct](json)
    result must be(Struct(Map(
      "a" -> Value(Value.Kind.ListValue(ListValue(List(
        Value(Value.Kind.NumberValue(1.0)),
        Value(Value.Kind.BoolValue(false)),
        Value(Value.Kind.NullValue(NullValue.NULL_VALUE)),
        Value(Value.Kind.StringValue("x"))
      ))))
    )))
  }

  "NullValue" should "be serialized and parsed from JSON correctly" in {
    JsonFormat.fromJsonString[StructTest]("""{"nv": null}""") must be(StructTest())
    JsonFormat.fromJsonString[StructTest]("""{"nv": "NULL_VALUE"}""") must be(StructTest())
    JsonFormat.fromJsonString[StructTest]("""{"nv": 0}""") must be(StructTest())
    JsonFormat.fromJsonString[StructTest]("""{"repNv": [null, 0, null]}""") must be(
      StructTest(repNv = Seq(NullValue.NULL_VALUE, NullValue.NULL_VALUE, NullValue.NULL_VALUE))
    )
  }
}
