package scalapb_jsoniter

import com.google.protobuf.ByteString
import jsontest.test3.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PrimitiveWrappersSpec extends AnyFlatSpec with Matchers {

  "Empty object" should "give empty json for Wrapper" in {
    JsonFormat.toJsonString(Wrapper()) must be("{}")
  }

  "primitive values" should "serialize properly" in {
    JsonFormat.toJsonString(Wrapper(wBool = Some(false))) must be("""{"wBool":false}""")
    JsonFormat.toJsonString(Wrapper(wBool = Some(true))) must be("""{"wBool":true}""")
    JsonFormat.toJsonString(Wrapper(wDouble = Some(3.1))) must be("""{"wDouble":3.1}""")
    JsonFormat.toJsonString(Wrapper(wFloat = Some(3.0f))) must be("""{"wFloat":3.0}""")
    JsonFormat.toJsonString(Wrapper(wInt32 = Some(35544))) must be("""{"wInt32":35544}""")
    JsonFormat.toJsonString(Wrapper(wInt32 = Some(0))) must be("""{"wInt32":0}""")
    JsonFormat.toJsonString(Wrapper(wInt64 = Some(125))) must be("""{"wInt64":"125"}""")
    JsonFormat.toJsonString(Wrapper(wUint32 = Some(125))) must be("""{"wUint32":125}""")
    JsonFormat.toJsonString(Wrapper(wUint64 = Some(125))) must be("""{"wUint64":"125"}""")
    JsonFormat.toJsonString(Wrapper(wString = Some("bar"))) must be("""{"wString":"bar"}""")
    JsonFormat.toJsonString(Wrapper(wString = Some(""))) must be("""{"wString":""}""")
    JsonFormat.toJsonString(Wrapper(wBytes = Some(ByteString.copyFrom(Array[Byte](3, 5, 4))))) must be(
      """{"wBytes":"AwUE"}"""
    )
    JsonFormat.toJsonString(Wrapper(wBytes = Some(ByteString.EMPTY))) must be("""{"wBytes":""}""")
    new Printer(formattingLongAsNumber = true).print(Wrapper(wUint64 = Some(125))) must be("""{"wUint64":125}""")
    new Printer(formattingLongAsNumber = true).print(Wrapper(wInt64 = Some(125))) must be("""{"wInt64":125}""")
  }

  "primitive values" should "parse properly" in {
    JsonFormat.fromJsonString[Wrapper]("""{"wBool":false}""") must be(Wrapper(wBool = Some(false)))
    JsonFormat.fromJsonString[Wrapper]("""{"wBool":true}""") must be(Wrapper(wBool = Some(true)))
    JsonFormat.fromJsonString[Wrapper]("""{"wDouble":3.1}""") must be(Wrapper(wDouble = Some(3.1)))
    JsonFormat.fromJsonString[Wrapper]("""{"wFloat":3.0}""") must be(Wrapper(wFloat = Some(3.0f)))
    JsonFormat.fromJsonString[Wrapper]("""{"wInt32":35544}""") must be(Wrapper(wInt32 = Some(35544)))
    JsonFormat.fromJsonString[Wrapper]("""{"wInt32":0}""") must be(Wrapper(wInt32 = Some(0)))
    JsonFormat.fromJsonString[Wrapper]("""{"wInt64":"125"}""") must be(Wrapper(wInt64 = Some(125)))
    JsonFormat.fromJsonString[Wrapper]("""{"wUint32":125}""") must be(Wrapper(wUint32 = Some(125)))
    JsonFormat.fromJsonString[Wrapper]("""{"wUint64":"125"}""") must be(Wrapper(wUint64 = Some(125)))
    JsonFormat.fromJsonString[Wrapper]("""{"wString":"bar"}""") must be(Wrapper(wString = Some("bar")))
    JsonFormat.fromJsonString[Wrapper]("""{"wString":""}""") must be(Wrapper(wString = Some("")))
    JsonFormat.fromJsonString[Wrapper]("""{"wBytes":"AwUE"}""") must be(
      Wrapper(wBytes = Some(ByteString.copyFrom(Array[Byte](3, 5, 4))))
    )
    JsonFormat.fromJsonString[Wrapper]("""{"wBytes":""}""") must be(
      Wrapper(wBytes = Some(ByteString.EMPTY))
    )
  }
}
