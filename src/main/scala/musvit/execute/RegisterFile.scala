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

class RegisterFileWritePort(config: MusvitConfig) extends Bundle {
  val rd = Input(UInt(REG_ADDR_WIDTH.W))
  val data = Input(UInt(WORD_WIDTH.W))
  val en = Input(Bool())
}

class RegisterFileIO(config: MusvitConfig) extends Bundle {
  val read = Vec(config.fetchWidth, new RegisterFileReadPort(config))
  val write = Vec(config.fetchWidth, new RegisterFileWritePort(config))
}

class RegisterFile(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new RegisterFileIO(config))

  val rf = Reg(Vec(NUM_OF_REGS, UInt(WORD_WIDTH.W)))

  for (i <- 0 until config.fetchWidth) {
    io.read(i).data1 := rf(io.read(i).rs1)
    io.read(i).data2 := rf(io.read(i).rs2)

    when (io.write(i).en ) {
      rf(io.write(i).rd) := io.write(i).data
    }
  }

  rf(0) := 0.U // Hardwire to zero
}