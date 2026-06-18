package scalapb_jsoniter

// Vendored (trimmed) from scalapb-json-common (io.github.scalapb-json) so this
// library can compile against scalapb-runtime 1.0.0-alpha.x, which upstream does
// not yet target.
object WellKnownTypes {
  val NANOS_PER_SECOND = 1000000000
  val NANOS_PER_MILLISECOND = 1000000
  val NANOS_PER_MICROSECOND = 1000

  def formatNanos(nanos: Int): String = {
    // Determine whether to use 3, 6, or 9 digits for the nano part.
    if (nanos % NANOS_PER_MILLISECOND == 0) {
      "%1$03d".format(nanos / NANOS_PER_MILLISECOND)
    } else if (nanos % NANOS_PER_MICROSECOND == 0) {
      "%1$06d".format(nanos / NANOS_PER_MICROSECOND)
    } else {
      "%1$09d".format(nanos)
    }
  }
}
