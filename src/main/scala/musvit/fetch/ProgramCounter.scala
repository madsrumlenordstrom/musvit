package musvit.fetch

import chisel3._
import utility.Constants._
import chisel3.util.RegEnable
import musvit.MusvitConfig

class ProgramCounterWritePort extends Bundle {
  val en = Bool()
  val data = UInt(ADDR_WIDTH.W)
}

class ProgramCounterIO extends Bundle {
  val enable     = Input(Bool())
  val pc         = Output(UInt(ADDR_WIDTH.W))
  val write      = Input(new ProgramCounterWritePort)
}

class ProgramCounter(config: MusvitConfig) extends Module {
  val io = IO(new ProgramCounterIO)

  val nextPC = Wire(UInt(ADDR_WIDTH.W))

  val pcReg = RegEnable(nextPC, config.resetPC.U, io.enable || io.write.en)
  
  nextPC := Mux(io.write.en, io.write.data, pcReg + (config.issueWidth * BYTES_PER_INST).U)

  io.pc := pcReg
}

object ProgramCounter {
  def apply(config: MusvitConfig) = {
    Module(new ProgramCounter(config))
  }
}