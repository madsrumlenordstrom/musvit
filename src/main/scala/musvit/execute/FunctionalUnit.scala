package musvit.execute

import chisel3._
import chisel3.util._

import musvit.common.OpCodes
import musvit.MusvitConfig
import utility.Constants._

class FunctionalUnitIO(config: MusvitConfig) extends Bundle with OpCodes {
  val rs = Flipped(Decoupled(new ReservationStationOperands(config)))
  val result = Decoupled(UInt(WORD_WIDTH.W))
  //val cdb = 
}

class FunctionalUnit(config: MusvitConfig) extends Module with OpCodes {
  val io = IO(new FunctionalUnitIO(config))
}
