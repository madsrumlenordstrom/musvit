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
  val en = Input(Bool())
  val robTag = Input(ROBTag(config))
}

class RegisterMapTableClearPort(config: MusvitConfig) extends Bundle {
  val rs = Input(UInt(REG_ADDR_WIDTH.W))
  val clear = Input(Bool())
  val robTag = Input(ROBTag(config))
}

class RegisterMapTableIO(config: MusvitConfig) extends Bundle {
  val read = Vec(config.fetchWidth, new RegisterMapTableReadPort(config))
  val write = Vec(config.fetchWidth, new RegisterMapTableWritePort(config))
  val clear = Vec(config.fetchWidth, new RegisterMapTableClearPort(config))
  val flush = Input(Bool())
}

class RegisterMapTable(config: MusvitConfig) extends Module {
  val io = IO(new RegisterMapTableIO(config))

  val regMap = RegInit(0.U.asTypeOf(Vec(NUM_OF_REGS, Valid(ROBTag(config)))))

  for (i <- 0 until config.fetchWidth) {
    // Read
    io.read(i).robTag1 := regMap(io.read(i).rs1)
    io.read(i).robTag2 := regMap(io.read(i).rs2)

    // Write
    when (io.write(i).en) {
      regMap(io.write(i).rs).bits := io.write(i).robTag
      regMap(io.write(i).rs).valid := true.B
    }

    // Clear valid bit (used for committing)
    when (io.clear(i).clear && io.clear(i).robTag === regMap(io.clear(i).rs).bits) { regMap(io.clear(i).rs).valid := false.B }
  }

  // Mark all as invalid on flush
  for (i <- 0 until NUM_OF_REGS) {
    when (io.flush) { regMap(i).valid := false.B }
  }

  // Always mark as invalid
  regMap(0).valid := false.B
}