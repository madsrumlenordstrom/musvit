package musvit.execute

import chisel3._
import chisel3.util._

import musvit.common.ControlSignals
import musvit.MusvitConfig
import utility.Constants._

class FunctionalUnitIO(config: MusvitConfig) extends Bundle {
  val result = Decoupled(CommonDataBus(config))
}

class FunctionalUnit(config: MusvitConfig, tag: Int) extends ReservationStation(config) with ControlSignals {
  val fu = IO(new FunctionalUnitIO(config))
  fu.result.bits.tag := tag.U

  val op    = rsReg.op
  val data1 = rsReg.src1.data
  val data2 = rsReg.src2.data
  val imm   = rsReg.imm

  // Mark operation as done
  when (fu.result.fire) {
    busyReg := false.B
  }
}
