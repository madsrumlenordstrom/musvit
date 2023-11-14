package musvit.execute

import chisel3._
import chisel3.util._

import musvit.common.ControlValues
import musvit.MusvitConfig
import utility.Constants._

class FunctionalUnitIO(config: MusvitConfig) extends Bundle {
  val result = Decoupled(CommonDataBus(config))
}

class FunctionalUnit(config: MusvitConfig) extends ReservationStation(config) with ControlValues {
  val fu = IO(new FunctionalUnitIO(config))
  fu.result.bits.tag := rsReg.robTag

  val op    = rsReg.op
  val data1 = rsReg.src1.data.bits
  val data2 = rsReg.src2.data.bits
  val imm   = rsReg.imm

  // Mark operation as done
  when (fu.result.fire) {
    busyReg := rs.writeEn
    rs.ready := true.B
  }
}
