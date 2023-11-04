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
  val result = Decoupled(CommonDataBus(config))
}

class FunctionalUnit(config: MusvitConfig, tag: Int) extends ReservationStation(config, tag) with OpCodes {
  val fu = IO(new FunctionalUnitIO(config))
  fu.result.bits.tag := tag.U

  val op    = rsReg.op
  val data1 = rsReg.fields(0).data
  val data2 = rsReg.fields(1).data

  // Mark operation as done
  when (fu.result.fire) {
    busyReg := false.B
  }
}
