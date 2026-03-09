package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import jsontest.anytests.AnyTest
import jsontest.custom_collection.{Guitar, Studio}
import jsontest.issue315.{Bar, Foo, Msg}
import jsontest.test.MyEnum
import jsontest.test3.MyTest3.MyEnum3
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import scalapb.{GeneratedEnum, GeneratedEnumCompanion, GeneratedMessage, GeneratedMessageCompanion}
import scalapb_jsoniter.codec.*

class CodecSpec extends AnyFreeSpec with Matchers {

  "GeneratedMessage" - {
    "encode to same value via codec and JsonFormat" in {
      def check[M <: GeneratedMessage: GeneratedMessageCompanion](m: M): Assertion = {
        val encodedWithJsonFormat = JsonFormat.toJsonString(m)
        implicit val c: JsonValueCodec[M] = generatedMessageCodec[M]
        val encodedImplicitly = writeToString(m)
        encodedImplicitly mustBe encodedWithJsonFormat
      }

      check(AnyTest("foo"))
      check(Guitar(99))
      check(Studio(Set(Guitar(1), Guitar(2), Guitar(3))))
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Foo(Foo("fooooo"))
        )
      )
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Bar(Bar("fooooo"))
        )
      )
    }

    "decode to same value via codec and JsonFormat" in {
      def check[M <: GeneratedMessage: GeneratedMessageCompanion](m: M): Assertion = {
        val json = JsonFormat.toJsonString(m)
        val decodedWithJsonFormat = JsonFormat.fromJsonString[M](json)
        implicit val c: JsonValueCodec[M] = generatedMessageCodec[M]
        val decodedImplicitly = readFromString[M](json)
        decodedImplicitly mustBe decodedWithJsonFormat
      }

      check(AnyTest("foo"))
      check(Guitar(99))
      check(Studio(Set(Guitar(1), Guitar(2), Guitar(3))))
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Foo(Foo("fooooo"))
        )
      )
      check(
        Msg(
          baz = "bazzz",
          someUnion = Msg.SomeUnion.Bar(Bar("fooooo"))
        )
      )
    }

    "encode using an implicit printer w/ non-standard settings" in {
      implicit val p: Printer = new Printer(includingDefaultValueFields = true)

      val g = Guitar(0)

      // Using regular JsonFormat yields an empty object because 0 is the default value.
      JsonFormat.toJsonString(g) mustBe "{}"

      // Using the codec with custom printer includes the default value.
      implicit val c: JsonValueCodec[Guitar] = generatedMessageCodec[Guitar]
      val json = writeToString(g)
      json mustBe """{"numberOfStrings":0}"""
    }

    "decode using an implicit parser w/ non-standard settings" in {
      implicit val pa: Parser = new Parser(preservingProtoFieldNames = true)

      val json = """{"number_of_strings":42}"""

      // Using the regular JsonFormat parser decodes to the defaultInstance.
      JsonFormat.fromJsonString[Guitar](json) mustBe Guitar.defaultInstance

      // Using the codec with custom parser decodes correctly.
      implicit val c: JsonValueCodec[Guitar] = generatedMessageCodec[Guitar]
      readFromString[Guitar](json) mustBe Guitar(42)
    }
  }

  "GeneratedEnum" - {
    "encode to same value via codec and JsonFormat" in {
      def check[E <: GeneratedEnum: GeneratedEnumCompanion](e: E): Assertion = {
        implicit val c: JsonValueCodec[E] = generatedEnumCodec[E]
        val encoded = writeToString(e)
        encoded mustBe s""""${e.name}""""
      }
      MyEnum.values.foreach(check(_: MyEnum))
      MyEnum3.values.foreach(check(_: MyEnum3))

      // Roundtrip via codec
      {
        implicit val c: JsonValueCodec[MyEnum] = generatedEnumCodec[MyEnum]
        readFromString[MyEnum](writeToString[MyEnum](MyEnum.V1)) mustBe MyEnum.V1
        readFromString[MyEnum](writeToString[MyEnum](MyEnum.V2)) mustBe MyEnum.V2
        readFromString[MyEnum](writeToString[MyEnum](MyEnum.V3)) mustBe MyEnum.V3
      }
      {
        implicit val c3: JsonValueCodec[MyEnum3] = generatedEnumCodec[MyEnum3]
        readFromString[MyEnum3](writeToString[MyEnum3](MyEnum3.V1)) mustBe MyEnum3.V1
        readFromString[MyEnum3](writeToString[MyEnum3](MyEnum3.V2)) mustBe MyEnum3.V2
        readFromString[MyEnum3](writeToString[MyEnum3](MyEnum3.V3)) mustBe MyEnum3.V3
      }
    }

    "decode to same value via codec and JsonFormat" in {
      def check[E <: GeneratedEnum: GeneratedEnumCompanion](e: E): Assertion = {
        implicit val c: JsonValueCodec[E] = generatedEnumCodec[E]
        val json = writeToString(e)
        val decodedImplicitly = readFromString[E](json)
        // The codec should decode to the same enum value
        decodedImplicitly mustBe e
      }
      MyEnum.values.foreach(check(_: MyEnum))
      MyEnum3.values.foreach(check(_: MyEnum3))
    }

    "encode using an implicit printer w/ non-standard settings" in {
      implicit val p: Printer = new Printer(formattingEnumsAsNumber = true)
      def check[E <: GeneratedEnum: GeneratedEnumCompanion](e: E): Assertion = {
        implicit val c: JsonValueCodec[E] = generatedEnumCodec[E]
        writeToString(e) mustBe e.value.toString
      }
      MyEnum.values.foreach(check(_: MyEnum))
      MyEnum3.values.foreach(check(_: MyEnum3))
    }
  }

  "Case class with GeneratedMessage and GeneratedEnum" - {
    "derive and use a codec via JsonCodecMaker" in {
      import CodecSpec._
      val band = Band(MyEnum.V1, Seq(Guitar(4), Guitar(5)))
      val expectedJson = """{"version":"V1","guitars":[{"numberOfStrings":4},{"numberOfStrings":5}]}"""
      writeToString(band)(bandCodec) mustBe expectedJson
      readFromString[Band](expectedJson)(bandCodec) mustBe band
    }
  }
}

object CodecSpec {
  case class Band(version: MyEnum, guitars: Seq[Guitar])

  // Implicit codecs for GeneratedMessage/GeneratedEnum must be in scope
  // before JsonCodecMaker.make derives the Band codec
  implicit val guitarCodec: JsonValueCodec[Guitar] = generatedMessageCodec[Guitar]
  implicit val myEnumCodec: JsonValueCodec[MyEnum] = generatedEnumCodec[MyEnum]
  val bandCodec: JsonValueCodec[Band] = JsonCodecMaker.make
}
