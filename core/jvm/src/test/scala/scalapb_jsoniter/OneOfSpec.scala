package scalapb_jsoniter

import com.google.protobuf.util.JsonFormat.{printer => ProtobufJavaPrinter}
import jsontest.oneof_test.OneOf._
import jsontest.oneof_test.{OneOf, OneOfMessage}
import org.scalatest.prop._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class OneOfSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks with JavaAssertions {

  val examples = Table(
    ("message", "json"),
    (OneOf.defaultInstance, "{}"),
    (OneOf(Field.Empty), "{}"),
    (OneOf(Field.Primitive("")), """{"primitive":""}"""),
    (OneOf(Field.Primitive("test")), """{"primitive":"test"}"""),
    (OneOf(Field.Wrapper("")), """{"wrapper":""}"""),
    (OneOf(Field.Wrapper("test")), """{"wrapper":"test"}"""),
    (OneOf(Field.Message(OneOfMessage())), """{"message":{}}"""),
    (OneOf(Field.Message(OneOfMessage(Some("test")))), """{"message":{"field":"test"}}""")
  )

  forEvery(examples) { (message: OneOf, json: String) =>
    new Printer(includingDefaultValueFields = false).print(message) must be(json)
    assertJsonEquals(
      new Printer(includingDefaultValueFields = false).print(message),
      ProtobufJavaPrinter().print(toJavaProto(message))
    )

    new Printer(includingDefaultValueFields = true).print(message) must be(json)
    assertJsonEquals(
      new Printer(includingDefaultValueFields = true).print(message),
      ProtobufJavaPrinter().includingDefaultValueFields().print(toJavaProto(message))
    )
  }

}
