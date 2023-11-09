package musvit.execute

import chisel3._
import chisel3.util._
import utility.Constants._
import musvit.MusvitConfig

class RegisterMapTableReadPort(config: MusvitConfig) extends Bundle {
  val rs1 = Input(UInt(REG_ADDR_WIDTH.W))
  val rs2 = Input(UInt(REG_ADDR_WIDTH.W))
  val robTag1 = Valid(ROBTag(config))
  val robTag2 = Valid(ROBTag(config))
}

class RegisterMapTableWritePort(config: MusvitConfig) extends Bundle {
  val rs = Input(UInt(REG_ADDR_WIDTH.W))
  val robTag = Flipped(Valid(ROBTag(config)))
}

class RegisterMapTableIO(config: MusvitConfig) extends Bundle {
  val read = Vec(config.fetchWidth, new RegisterMapTableReadPort(config))
  val write = Vec(config.fetchWidth, new RegisterMapTableWritePort(config))
  val flush = Input(Bool())
}

class RegisterMapTable(config: MusvitConfig) extends Module {
  val io = IO(new RegisterMapTableIO(config))

  val regMap = RegInit(0.U.asTypeOf(Vec(NUM_OF_REGS, Valid(ROBTag(config)))))

  for (i <- 0 until config.fetchWidth) {
    io.read(i).robTag1 := regMap(io.read(i).rs1)
    io.read(i).robTag2 := regMap(io.read(i).rs2)
  }

  // Mark all as invalid on flush
  for (i <- 0 until NUM_OF_REGS) {
    when (io.flush) { regMap(i).valid := false.B }
  }

  // Always mark as invalid
  regMap(0).valid := false.B
}