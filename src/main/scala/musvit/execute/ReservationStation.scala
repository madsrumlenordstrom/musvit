package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import musvit.common.OpCodes
import utility.Constants._
import dataclass.data

class ReservationStationFields(config: MusvitConfig) extends Bundle with OpCodes {
  val op = UInt(OP_WIDTH.W)
  val qj = UInt(log2Up(config.rsNum).W)
  val qk = UInt(log2Up(config.rsNum).W)
  val vj = UInt(WORD_WIDTH.W)
  val vk = UInt(WORD_WIDTH.W)
  //val imm = UInt(WORD_WIDTH.W)
  val busy = Bool()
}

// Maybe overkill with a bundle??
class CDBMonitorIO(config: MusvitConfig) extends Bundle {
  val vi = UInt(WORD_WIDTH.W)
}

class ReservationStationOperands(config: MusvitConfig) extends Bundle with OpCodes {
  val op = UInt(OP_WIDTH.W)
  val op1 = UInt(WORD_WIDTH.W)
  val op2 = UInt(WORD_WIDTH.W)
}

class ReservationStationIO(config: MusvitConfig) extends Bundle with OpCodes {
  val fields = Flipped(Decoupled(new ReservationStationFields(config)))
  val monitor = Vec(config.rsNum, Flipped(Decoupled(new CDBMonitorIO(config))))
  val operands = Decoupled(new ReservationStationOperands(config))
}

class ReservationStation(config: MusvitConfig, uniqID: UInt) extends Module {
  val io = IO(new ReservationStationIO(config))

  val dataReg = RegInit(0.U.asTypeOf(new ReservationStationFields(config)))
  val op1Valid = dataReg.qj === 0.U
  val op2Valid = dataReg.qk === 0.U
  ///val uniqID = uniqID

  // Get data
  when (io.fields.valid && !dataReg.busy) {
    dataReg := io.fields.bits
  }

  io.fields.ready := !dataReg.busy
  io.monitor.map(_.ready.:=(!op1Valid && !op2Valid)) // Not really used

  // Monitor bus for operands
  when (io.monitor(dataReg.qj).valid && !op1Valid) {
    dataReg.vj := io.monitor(dataReg.qj).bits.vi
    dataReg.qj := 0.U
  }
  when (io.monitor(dataReg.qk).valid && !op2Valid) {
    dataReg.vk := io.monitor(dataReg.qk).bits.vi
    dataReg.qk := 0.U
  }

  val validData = dataReg.busy && op1Valid && op2Valid

  // Flag operands as consumed
  when (validData && io.operands.ready) {
    io.fields.ready := true.B
    dataReg.busy := false.B
  }

  io.operands.valid := validData
  io.operands.bits.op := dataReg.op
  io.operands.bits.op1 := dataReg.vj
  io.operands.bits.op2 := dataReg.vk
}
