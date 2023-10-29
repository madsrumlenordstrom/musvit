package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import utility.Constants._
import musvit.common.OpCodes

object ReservationStationTag {
  def apply(config: MusvitConfig) = {
    UInt(log2Up(config.fuNum * config.rsPerFu + 1).W)
  }
}

class TaggedData[T <: Data](gen: T, config: MusvitConfig) extends Bundle {
  val tag = ReservationStationTag(config)
  val data = gen
}

object TaggedData {
  def apply[T <: Data](gen: T, config: MusvitConfig): TaggedData[T] = new TaggedData(gen, config)
}

object CommonDataBus {
  def apply(config: MusvitConfig) = {
    TaggedData(UInt(WORD_WIDTH.W), config)
  }
}

class IssueBusFields(config: MusvitConfig) extends Bundle with OpCodes {
  val op = UInt(OP_WIDTH.W)
  val fields = Vec(2, CommonDataBus(config))
}

object IssueBus {
  def apply(config: MusvitConfig) = {
    TaggedData(new IssueBusFields(config), config)
  }
}


