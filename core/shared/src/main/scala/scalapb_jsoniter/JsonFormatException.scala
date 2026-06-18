package scalapb_jsoniter

case class JsonFormatException(msg: String, cause: Throwable) extends Exception(msg, cause) {
  def this(msg: String) = this(msg, null)
}
