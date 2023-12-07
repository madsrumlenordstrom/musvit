package musvit.fetch

import chisel3._
import chisel3.util._

import utility.Constants._
import musvit.MusvitConfig

class ProgramCounterWritePort extends Bundle {
  val en = Bool()
  val data = UInt(ADDR_WIDTH.W)
}

class ProgramCounterIO(config: MusvitConfig) extends Bundle {
  val enable     = Input(Bool())
  val branch     = Input(new BranchTargetBufferWritePort(config))
  val pc         = Output(UInt(ADDR_WIDTH.W))
  val branched   = Output(Vec(config.issueWidth, Bool()))
}

class ProgramCounter(config: MusvitConfig) extends Module {
  val io = IO(new ProgramCounterIO(config))

  val nextPC = Wire(UInt(ADDR_WIDTH.W))

  val pcReg = RegEnable(nextPC, config.resetPC.U, io.enable || io.branch.en)

  // Branch target buffer
  val btb = BranchTargetBuffer(config)
  btb.io.write <> io.branch
  btb.io.read.pc := pcReg
  io.branched := Mux(btb.io.read.target.valid && !io.branch.en,
  VecInit(UIntToOH(btb.io.read.chosen, config.issueWidth).asBools),
  VecInit.fill(config.issueWidth)(false.B))
  
  nextPC := MuxCase(pcReg + (config.issueWidth * BYTES_PER_INST).U, Seq(
    (io.branch.en) -> (io.branch.target),
    (btb.io.read.target.valid) -> (btb.io.read.target.bits)
  ))

  io.pc := pcReg
}

object ProgramCounter {
  def apply(config: MusvitConfig) = {
    Module(new ProgramCounter(config))
  }
}