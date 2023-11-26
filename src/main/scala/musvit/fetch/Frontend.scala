package musvit.fetch

import chisel3._
import chisel3.util._

import musvit.Musvit
import musvit.MusvitConfig
import musvit.common.ControlSignals
import memory.MusvitROMIO
import utility.Constants._

class FetchPacket(config: MusvitConfig) extends Bundle {
  val insts = Vec(config.issueWidth, UInt(INST_WIDTH.W))
  val branched = Vec(config.issueWidth, Bool())
  val pc = UInt(ADDR_WIDTH.W)
}

class MicroOperation(config: MusvitConfig) extends Bundle {
  val ctrl = new ControlSignals
  val branched = Bool()
  val inst = UInt(INST_WIDTH.W)
}

class MicroOperationPacket(config: MusvitConfig) extends Bundle {
  val microOps = Vec(config.issueWidth, new MicroOperation(config))
  val pc  = UInt(ADDR_WIDTH.W)
}

class FrontendIO(config: MusvitConfig) extends Bundle {
  val read = Flipped(new MusvitROMIO(config))
  val pc = Input(new ProgramCounterWritePort())
  val flush = Input(Bool())
  val mop = Decoupled(new MicroOperationPacket(config))
}

class Frontend(config: MusvitConfig) extends Module {
  val io = IO(new FrontendIO(config))
  val fp = Wire(Decoupled(new FetchPacket(config)))

  // Program counter
  val pc = ProgramCounter(config)
  pc.io.enable := fp.fire
  pc.io.write <> io.pc
  
  // Instruction memory
  io.read.addr := pc.io.pc
  io.read.data.ready := fp.ready
  
  // Create instruction packet
  fp.valid := io.read.data.valid && !io.flush
  fp.bits.insts <> io.read.data.bits
  fp.bits.pc := pc.io.pc
  fp.bits.branched := VecInit(Seq.fill(config.issueWidth)(false.B)) // TODO

  // Instruction queue
  val iq = Module(new InstructionQueue(config))
  iq.io.in <> fp
  iq.io.flush := io.flush

  // Decode instructions
  val decoders = Seq.fill(config.issueWidth)(Module(new Decode(config)))
  for (i <- 0 until config.issueWidth) {
    decoders(i).io.inst := iq.io.out.bits.insts(i)
    io.mop.bits.microOps(i).ctrl := decoders(i).io.ctrl
    io.mop.bits.microOps(i).branched := iq.io.out.bits.branched(i)
    io.mop.bits.microOps(i).inst := iq.io.out.bits.insts(i)
  }
  io.mop.bits.pc := iq.io.out.bits.pc
  io.mop.valid := iq.io.out.valid
  iq.io.out.ready := io.mop.ready
}

object Frontend {
  def apply(config: MusvitConfig) = {
    Module(new Frontend(config))
  }
}
