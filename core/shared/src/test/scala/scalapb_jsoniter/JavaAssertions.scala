package scalapb_jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import scalapb_json.ScalapbJsonCommon.GenericCompanion
import scalapb.GeneratedMessageCompanion
import org.scalatest.matchers.must.Matchers
import scalapb_json.*

trait JavaAssertions extends JavaAssertionsPlatform {
  self: Matchers =>

  def registeredCompanions: Seq[GeneratedMessageCompanion[?]] = Seq.empty

  val ScalaTypeRegistry = registeredCompanions.foldLeft(TypeRegistry.empty)((r, c) =>
    r.addMessageByCompanion(c.asInstanceOf[GenericCompanion])
  )
  val ScalaJsonParser = new Parser(typeRegistry = ScalaTypeRegistry)
  val ScalaJsonPrinter = new Printer(typeRegistry = ScalaTypeRegistry)

  protected def parseJson(s: String): Any = {
    val codec = new JsonValueCodec[Any] {
      def nullValue: Any = null
      def encodeValue(x: Any, out: JsonWriter): Unit = {
        x match {
          case null            => out.writeNull()
          case b: Boolean      => out.writeVal(b)
          case s: String       => out.writeVal(s)
          case l: Long         => out.writeVal(l)
          case i: Int          => out.writeVal(i)
          case d: Double       => out.writeVal(d)
          case bd: BigDecimal  => out.writeVal(bd)
          case m: Map[?, ?] =>
            out.writeObjectStart()
            m.asInstanceOf[Map[String, Any]].foreach { case (k, v) =>
              out.writeKey(k)
              encodeValue(v, out)
            }
            out.writeObjectEnd()
          case v: Vector[?] =>
            out.writeArrayStart()
            v.foreach(e => encodeValue(e.asInstanceOf[Any], out))
            out.writeArrayEnd()
        }
      }
      def decodeValue(in: JsonReader, default: Any): Any = readAny(in)
      private def readAny(in: JsonReader): Any = {
        val b = in.nextToken()
        b match {
          case '"' =>
            in.rollbackToken()
            in.readString(null)
          case 't' | 'f' =>
            in.rollbackToken()
            in.readBoolean()
          case 'n' =>
            in.rollbackToken()
            in.skip()
            null
          case '{' =>
            if (in.isNextToken('}')) {
              Map.empty[String, Any]
            } else {
              in.rollbackToken()
              val m = scala.collection.mutable.LinkedHashMap.empty[String, Any]
              while ({
                m += (in.readKeyAsString() -> readAny(in))
                in.isNextToken(',')
              }) ()
              m.toMap
            }
          case '[' =>
            if (in.isNextToken(']')) {
              Vector.empty[Any]
            } else {
              in.rollbackToken()
              val arr = scala.collection.mutable.ArrayBuffer.empty[Any]
              while ({
                arr += readAny(in)
                in.isNextToken(',')
              }) ()
              arr.toVector
            }
          case _ =>
            in.rollbackToken()
            val raw = in.readRawValAsBytes()
            val str = new String(raw)
            try {
              val l = str.toLong
              l
            } catch {
              case _: NumberFormatException =>
                try { BigDecimal(str) }
                catch { case _: NumberFormatException => str.toDouble }
            }
        }
      }
    }
    readFromString[Any](s)(codec)
  }

  protected def assertJsonEquals(actual: String, expected: String): Unit = {
    val a = parseJson(actual)
    val b = parseJson(expected)
    assert(a == b, s"JSON mismatch:\n  actual:   $actual\n  expected: $expected")
  }
}
