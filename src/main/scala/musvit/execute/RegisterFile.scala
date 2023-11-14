package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import utility.Constants._
import musvit.common.ControlValues

class RegisterFileReadPort(config: MusvitConfig) extends Bundle {
  val rs1 = Input(UInt(REG_ADDR_WIDTH.W))
  val rs2 = Input(UInt(REG_ADDR_WIDTH.W))
  val data1 = Output(UInt(WORD_WIDTH.W))
  val data2 = Output(UInt(WORD_WIDTH.W))
}

class RegisterFileIO(config: MusvitConfig) extends Bundle {
  val read = Vec(config.fetchWidth, new RegisterFileReadPort(config))
  val commit = Decoupled(Vec(config.fetchWidth, CommitBus(config)))
}

class RegisterFile(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new RegisterFileIO(config))

  val rf = Reg(Vec(NUM_OF_REGS, UInt(WORD_WIDTH.W)))

  for (i <- 0 until config.fetchWidth) {
    io.read(i).data1 := rf(io.read(i).rs1)
    io.read(i).data2 := rf(io.read(i).rs2)

    when (io.commit.valid && io.commit.bits(i).wb === WB.REG) {
      rf(io.commit.bits(i).rd) := io.commit.bits(i).data
    }
  }

  rf(0) := 0.U // Hardwire to zero
}