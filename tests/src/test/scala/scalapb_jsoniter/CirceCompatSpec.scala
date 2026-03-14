package scalapb_jsoniter

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import jsontest.test.*
import jsontest.test3.*
import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.struct.*
import com.google.protobuf.field_mask.FieldMask
import scalapb.GeneratedMessage

class CirceCompatSpec extends AnyFunSpec with Matchers {

  val jsoniterPrinter = new scalapb_jsoniter.Printer()
  val jsoniterParser = new scalapb_jsoniter.Parser()
  val circePrinter = new scalapb_circe.Printer()
  val circeParser = new scalapb_circe.Parser()

  def jsoniterEncode[A <: GeneratedMessage](m: A): String = jsoniterPrinter.print(m)
  def jsoniterDecode[A <: GeneratedMessage](s: String)(implicit cmp: scalapb.GeneratedMessageCompanion[A]): A =
    jsoniterParser.fromJsonString[A](s)

  def circeEncode[A <: GeneratedMessage](m: A): String = circePrinter.print(m)
  def circeDecode[A <: GeneratedMessage](s: String)(implicit cmp: scalapb.GeneratedMessageCompanion[A]): A =
    circeParser.fromJsonString[A](s)

  def testRoundTrip[A <: GeneratedMessage](name: String, msg: A)(implicit cmp: scalapb.GeneratedMessageCompanion[A]): Unit = {
    it(s"$name: jsoniter encode -> circe decode") {
      val json = jsoniterEncode(msg)
      val decoded = circeDecode[A](json)
      decoded shouldBe msg
    }

    it(s"$name: circe encode -> jsoniter decode") {
      val json = circeEncode(msg)
      val decoded = jsoniterDecode[A](json)
      decoded shouldBe msg
    }
  }

  describe("scalapb-jsoniter / scalapb-circe compatibility") {

    val testProto = MyTest().update(
      _.hello := "Foo",
      _.foobar := 37,
      _.primitiveSequence := Seq("a", "b", "c"),
      _.repMessage := Seq(MyTest(), MyTest(hello = Some("h11"))),
      _.optMessage := MyTest().update(_.foobar := 39),
      _.stringToInt32 := Map("foo" -> 14, "bar" -> 19),
      _.intToMytest := Map(14 -> MyTest(), 35 -> MyTest(hello = Some("boo"))),
      _.repEnum := Seq(MyEnum.V1, MyEnum.V2, MyEnum.UNKNOWN),
      _.optEnum := MyEnum.V2,
      _.intToEnum := Map(32 -> MyEnum.V1, 35 -> MyEnum.V2),
      _.stringToBool := Map("ff" -> false, "tt" -> true),
      _.boolToString := Map(false -> "ff", true -> "tt"),
      _.optBool := false
    )

    testRoundTrip("MyTest with various fields", testProto)
    testRoundTrip("empty MyTest", MyTest())
    testRoundTrip("MyTest with oneof", MyTest(trickOrTreat = MyTest.TrickOrTreat.Treat(MyTest(hello = Some("x")))))

    testRoundTrip("MyTest3 proto3", MyTest3(s = "hello", i32 = 42))
    testRoundTrip("empty MyTest3", MyTest3())
    testRoundTrip("MyTest3 with enum", MyTest3(optEnum = MyTest3.MyEnum3.V1))
    testRoundTrip("MyTest3 with oneof", MyTest3(trickOrTreat = MyTest3.TrickOrTreat.Treat(MyTest3(s = "x"))))

    testRoundTrip("Duration", Duration(seconds = 123, nanos = 456000000))
    testRoundTrip("Timestamp", Timestamp(seconds = 1709307600, nanos = 0))
    testRoundTrip("FieldMask", FieldMask(paths = Seq("foo.bar", "baz")))

    testRoundTrip("Struct", Struct(
      Map(
        "key" -> Value(Value.Kind.StringValue("value")),
        "num" -> Value(Value.Kind.NumberValue(42.0)),
        "bool" -> Value(Value.Kind.BoolValue(true)),
        "null" -> Value(Value.Kind.NullValue(NullValue.NULL_VALUE)),
        "list" -> Value(Value.Kind.ListValue(ListValue(List(
          Value(Value.Kind.NumberValue(1.0)),
          Value(Value.Kind.StringValue("two"))
        )))),
        "nested" -> Value(Value.Kind.StructValue(Struct(
          Map("inner" -> Value(Value.Kind.StringValue("val")))
        )))
      )
    ))

    testRoundTrip("IntFields with various int types", IntFields(
      int = Some(42),
      long = Some(123456789L),
      uint = Some(100),
      ulong = Some(200L),
      sint = Some(-10),
      slong = Some(-20L),
      fixint = Some(300),
      fixlong = Some(400L)
    ))

    testRoundTrip("DoubleFloat", DoubleFloat(d = Some(3.14), f = Some(2.72f)))

    testRoundTrip("MyTest with bytes", MyTest(
      optBs = Some(com.google.protobuf.ByteString.copyFromUtf8("hello bytes"))
    ))

    testRoundTrip("MyTest with map of fixed64 to bytes", MyTest(
      fixed64ToBytes = Map(1L -> com.google.protobuf.ByteString.copyFromUtf8("val1"))
    ))

    describe("long as number compatibility") {
      val jsoniterLongPrinter = new scalapb_jsoniter.Printer(formattingLongAsNumber = true)
      val circeLongPrinter = new scalapb_circe.Printer(formattingLongAsNumber = true)

      it("jsoniter long-as-number -> circe decode") {
        val msg = MyTest(bazinga = Some(642))
        val json = jsoniterLongPrinter.print(msg)
        val decoded = circeDecode[MyTest](json)
        decoded shouldBe msg
      }

      it("circe long-as-number -> jsoniter decode") {
        val msg = MyTest(bazinga = Some(642))
        val json = circeLongPrinter.print(msg)
        val decoded = jsoniterDecode[MyTest](json)
        decoded shouldBe msg
      }
    }

    describe("enum as number compatibility") {
      val jsoniterEnumPrinter = new scalapb_jsoniter.Printer(formattingEnumsAsNumber = true)
      val circleEnumPrinter = new scalapb_circe.Printer(formattingEnumsAsNumber = true)

      it("jsoniter enum-as-number -> circe decode") {
        val msg = MyTest(optEnum = Some(MyEnum.V2))
        val json = jsoniterEnumPrinter.print(msg)
        val decoded = circeDecode[MyTest](json)
        decoded shouldBe msg
      }

      it("circe enum-as-number -> jsoniter decode") {
        val msg = MyTest(optEnum = Some(MyEnum.V2))
        val json = circleEnumPrinter.print(msg)
        val decoded = jsoniterDecode[MyTest](json)
        decoded shouldBe msg
      }
    }

    describe("preserving proto field names compatibility") {
      val jsoniterPreservedPrinter = new scalapb_jsoniter.Printer(preservingProtoFieldNames = true)
      val jsoniterPreservedParser = new scalapb_jsoniter.Parser(preservingProtoFieldNames = true)
      val circePreservedPrinter = new scalapb_circe.Printer(preservingProtoFieldNames = true)
      val circePreservedParser = new scalapb_circe.Parser(preservingProtoFieldNames = true)

      it("jsoniter preserved names -> circe decode") {
        val msg = MyTest(primitiveSequence = Seq("a", "b"), optBool = Some(true))
        val json = jsoniterPreservedPrinter.print(msg)
        val decoded = circePreservedParser.fromJsonString[MyTest](json)
        decoded shouldBe msg
      }

      it("circe preserved names -> jsoniter decode") {
        val msg = MyTest(primitiveSequence = Seq("a", "b"), optBool = Some(true))
        val json = circePreservedPrinter.print(msg)
        val decoded = jsoniterPreservedParser.fromJsonString[MyTest](json)
        decoded shouldBe msg
      }
    }
  }
}
