package scalapb_jsoniter

import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import jsontest.test.WellKnownTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class WellKnownTypesSpec extends AnyFlatSpec with Matchers with JavaAssertions {

  val durationProto = WellKnownTest(duration = Some(Duration(146, 3455)))

  "duration" should "serialize and parse correctly" in {
    val durationJson = """{
                         |  "duration": "146.000003455s"
                         |}""".stripMargin
    assertJsonEquals(JsonFormat.toJsonString(durationProto), durationJson)
    JsonFormat.fromJsonString[WellKnownTest](durationJson) must be(durationProto)
  }

  "timestamp" should "serialize and parse correctly" in {
    val timestampJson = """{
                          |  "timestamp": "2016-09-16T12:35:24.375123456Z"
                          |}""".stripMargin
    val timestampProto =
      WellKnownTest(timestamp = Some(Timestamp(seconds = 1474029324, nanos = 375123456)))
    JsonFormat.fromJsonString[WellKnownTest](timestampJson) must be(timestampProto)
    assertJsonEquals(JsonFormat.toJsonString(timestampProto), timestampJson)
  }
}
