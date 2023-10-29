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
  val dataReg = RegEnable(io.rs.bits, 0.U.asTypeOf(new FunctionalUnitOperands(config)), ready)
  val valid = RegEnable(io.rs.valid, false.B, ready)

  val op = dataReg.op
  val opr1 = dataReg.data1
  val opr2 = dataReg.data2
}
