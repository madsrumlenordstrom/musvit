package musvit.execute

import chisel3._
import chisel3.util._

import musvit.common.OpCodes
import musvit.MusvitConfig
import utility.Constants._

class FunctionalUnitOperands(config: MusvitConfig) extends Bundle with OpCodes {
  val op = UInt(OP_WIDTH.W)
  val data1 = UInt(WORD_WIDTH.W)
  val data2 = UInt(WORD_WIDTH.W)
}

class FunctionalUnitIO(config: MusvitConfig) extends Bundle {
  val rs = Flipped(Decoupled(new FunctionalUnitOperands(config)))
  val cdb = Decoupled(CommonDataBus(config))
}

class FunctionalUnit(config: MusvitConfig) extends Module with OpCodes {
  val io = IO(new FunctionalUnitIO(config))

  val ready = WireDefault(false.B)

  val valid = io.rs.valid
  val op = io.rs.bits.op
  val data1 = io.rs.bits.data1
  val data2 = io.rs.bits.data2
  io.rs.ready := ready
}
