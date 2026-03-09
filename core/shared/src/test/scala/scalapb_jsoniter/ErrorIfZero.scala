package scalapb_jsoniter

import scalapb.TypeMapper

case class ErrorIfZero(value: Int)

object ErrorIfZero {
  implicit val typeMapper: TypeMapper[Int, ErrorIfZero] = TypeMapper[Int, ErrorIfZero] { (value: Int) =>
    if (value == 0) throw new RuntimeException("Received zero")
    ErrorIfZero(value)
  }(_.value)
}
