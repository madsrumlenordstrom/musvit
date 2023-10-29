package musvit.execute

import chisel3._
import chisel3.util._

import musvit.common.OpCodes
import musvit.MusvitConfig
import utility.Constants._

class FunctionalUnitData(config: MusvitConfig) extends Bundle with OpCodes {
  val q = ReservationStationID(config)
  val op = UInt(OP_WIDTH.W)
  val op1 = UInt(WORD_WIDTH.W)
  val op2 = UInt(WORD_WIDTH.W)
}

class CommonDataBusData(config: MusvitConfig) extends Bundle with OpCodes {
  val qi = ReservationStationID(config)
  val vi = UInt(WORD_WIDTH.W)
}

class FunctionalUnitIO(config: MusvitConfig) extends Bundle {
  val rs = Flipped(Decoupled(new FunctionalUnitData(config)))
  val cdb = Decoupled(new CommonDataBusData(config))
}

class FunctionalUnit(config: MusvitConfig) extends Module with OpCodes {
  val io = IO(new FunctionalUnitIO(config))

  val dataReg = RegInit(0.U.asTypeOf(new FunctionalUnitData(config)))

  when (io.rs.valid && io.rs.ready) {
    dataReg := io.rs.bits
  }

  val q = dataReg.q
  val op = dataReg.op
  val op1 = dataReg.op1
  val op2 = dataReg.op2
}
