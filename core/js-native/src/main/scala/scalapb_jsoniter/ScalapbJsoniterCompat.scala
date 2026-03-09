package scalapb_jsoniter

import scalapb.GeneratedMessageCompanion

private[scalapb_jsoniter] object ScalapbJsoniterCompat {
  def getClassFromMessageCompanion(x: GeneratedMessageCompanion[?]): Class[?] =
    x.defaultInstance.getClass
}
