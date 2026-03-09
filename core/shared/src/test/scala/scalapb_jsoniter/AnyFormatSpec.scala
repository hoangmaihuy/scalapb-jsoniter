package scalapb_jsoniter

import com.google.protobuf.any.{Any => PBAny}
import jsontest.anytests.{AnyTest, ManyAnyTest}
import scalapb.GeneratedMessageCompanion
import scalapb_json.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class AnyFormatSpec extends AnyFlatSpec with Matchers with JavaAssertions {
  val RawExample = AnyTest("test")

  val RawJson = """{"field":"test"}"""

  val AnyExample = PBAny.pack(RawExample)

  val AnyJson = """{"@type":"type.googleapis.com/jsontest.AnyTest","field":"test"}"""

  val CustomPrefixAny = PBAny.pack(RawExample, "example.com/")

  val CustomPrefixJson = """{"@type":"example.com/jsontest.AnyTest","field":"test"}"""

  val ManyExample = ManyAnyTest(
    Seq(
      PBAny.pack(AnyTest("1")),
      PBAny.pack(AnyTest("2"))
    )
  )

  val ManyPackedJson =
    """|{
       |  "@type": "type.googleapis.com/jsontest.ManyAnyTest",
       |  "fields": [
       |    {"@type": "type.googleapis.com/jsontest.AnyTest", "field": "1"},
       |    {"@type": "type.googleapis.com/jsontest.AnyTest", "field": "2"}
       |  ]
       |}""".stripMargin

  override def registeredCompanions: Seq[GeneratedMessageCompanion[?]] = Seq(AnyTest, ManyAnyTest)

  // For clarity
  def UnregisteredPrinter = JsonFormat.printer
  def UnregisteredParser = JsonFormat.parser

  "Any" should "fail to serialize if its respective companion is not registered" in {
    an[IllegalStateException] must be thrownBy UnregisteredPrinter.print(AnyExample)
  }

  "Any" should "fail to deserialize if its respective companion is not registered" in {
    a[JsonFormatException] must be thrownBy UnregisteredParser.fromJsonString[PBAny](AnyJson)
  }

  "Any" should "serialize correctly if its respective companion is registered" in {
    ScalaJsonPrinter.print(AnyExample) must be(AnyJson)
  }

  "Any" should "fail to serialize with a custom URL prefix if specified" in {
    an[IllegalStateException] must be thrownBy ScalaJsonPrinter.print(CustomPrefixAny)
  }

  "Any" should "fail to deserialize for a non-Google-prefixed type URL" in {
    a[JsonFormatException] must be thrownBy ScalaJsonParser.fromJsonString[PBAny](CustomPrefixJson)
  }

  "Any" should "deserialize correctly if its respective companion is registered" in {
    ScalaJsonParser.fromJsonString[PBAny](AnyJson) must be(AnyExample)
  }

  "Any" should "resolve printers recursively" in {
    val packed = PBAny.pack(ManyExample)
    assertJsonEquals(ScalaJsonPrinter.print(packed), ManyPackedJson)
  }

  "Any" should "resolve parsers recursively" in {
    ScalaJsonParser.fromJsonString[PBAny](ManyPackedJson).unpack[ManyAnyTest] must be(ManyExample)
  }
}
