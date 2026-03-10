package scalapb_jsoniter

import scala.compiletime.testing.{typeCheckErrors, ErrorKind}
import org.scalatest.funspec.AnyFunSpec
import scalapb_jsoniter.ProtoMacrosJsoniter._

class ProtoMacrosJsoniterTest2 extends AnyFunSpec {

  describe("ProtoMacrosJsoniter scala 3 test") {
    inline def checkTypeError(
      src: String,
      expectMessage: String
    ) = {
      typeCheckErrors(src) match {
        case List(e) =>
          assert(e.kind == ErrorKind.Typer)
          assert(e.message == expectMessage)
        case other =>
          fail("unexpected " + other)
      }
    }

    it("struct") {
      checkTypeError(""" struct"null" """, "Expected an object")
      checkTypeError(""" struct"[3]" """, "Expected an object")
      checkTypeError(""" struct"true" """, "Expected an object")
      checkTypeError(""" struct"12345" """, "Expected an object")

      checkTypeError(""" struct" ] " """, "Expected an object")
    }

    it("value") {
      checkTypeError(""" value" ] " """, """illegal number, offset: 0x00000001, buf:
+----------+-------------------------------------------------+------------------+
|          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
+----------+-------------------------------------------------+------------------+
| 00000000 | 20 5d 20                                        |  ]               |
+----------+-------------------------------------------------+------------------+""")
      checkTypeError(""" value" } " """, """illegal number, offset: 0x00000001, buf:
+----------+-------------------------------------------------+------------------+
|          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
+----------+-------------------------------------------------+------------------+
| 00000000 | 20 7d 20                                        |  }               |
+----------+-------------------------------------------------+------------------+""")
    }
  }
}
