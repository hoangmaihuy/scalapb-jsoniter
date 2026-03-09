package scalapb_jsoniter

import scalapb.e2e.repeatables.RepeatablesTest
import scalapb.e2e.repeatables.RepeatablesTest.Nested
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class RepeatablesSpec extends AnyFlatSpec with Matchers {

  "fromJson" should "invert toJson single" in {
    val rep = RepeatablesTest(
      strings = Seq("s1", "s2"),
      ints = Seq(14, 19),
      doubles = Seq(3.14, 2.17),
      nesteds = Seq(Nested())
    )
    val j = JsonFormat.toJsonString(rep)
    JsonFormat.fromJsonString[RepeatablesTest](j) must be(rep)
  }

  "fromJson" should "invert toJson with all fields" in {
    val rep = RepeatablesTest(
      strings = Seq("hello", "world", ""),
      ints = Seq(0, 1, -1, Int.MaxValue, Int.MinValue),
      doubles = Seq(0.0, 1.5, -1.5, Double.MaxValue, Double.MinValue),
      nesteds = Seq(Nested(), Nested(Some(42)), Nested(Some(-1))),
      packedLongs = Seq(0L, 1L, -1L, Long.MaxValue, Long.MinValue),
      enums = Seq(RepeatablesTest.Enum.ONE, RepeatablesTest.Enum.TWO)
    )
    val j = JsonFormat.toJsonString(rep)
    JsonFormat.fromJsonString[RepeatablesTest](j) must be(rep)
  }

  "fromJson" should "invert toJson with empty repeated fields" in {
    val rep = RepeatablesTest()
    val j = JsonFormat.toJsonString(rep)
    JsonFormat.fromJsonString[RepeatablesTest](j) must be(rep)
  }

  "fromJson" should "invert toJson with single element repeated fields" in {
    val rep = RepeatablesTest(
      strings = Seq("single"),
      ints = Seq(42),
      doubles = Seq(3.14),
      nesteds = Seq(Nested(Some(7))),
      packedLongs = Seq(999L),
      enums = Seq(RepeatablesTest.Enum.TWO)
    )
    val j = JsonFormat.toJsonString(rep)
    JsonFormat.fromJsonString[RepeatablesTest](j) must be(rep)
  }
}
