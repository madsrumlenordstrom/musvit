package musvit.fetch

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import musvit.MusvitConfig
import utility.Constants._
import utility.DecoupledToValid

object BranchTargetBuffer {
  def apply(config: MusvitConfig): BranchTargetBuffer = {
    Module(new BranchTargetBuffer(config))
  }

  object State extends ChiselEnum {
    val strongTaken, weakTaken, weakNotTaken, strongNotTaken = Value
  }
}

class BranchTargetBufferWritePort(config: MusvitConfig) extends Bundle {
  val en     = Input(Bool())
  val taken  = Input(Bool())
  val pc     = Input(UInt(ADDR_WIDTH.W))
  val target = Input(UInt(ADDR_WIDTH.W))
}

class BranchTargetBufferReadPort(config: MusvitConfig) extends Bundle {
  val pc     = Input(UInt(ADDR_WIDTH.W))
  val target = Output(Valid(UInt(ADDR_WIDTH.W)))
  val chosen = Output(UInt(log2Up(config.issueWidth).W))
}

class BranchTargetBufferIO(config: MusvitConfig) extends Bundle {
  val read  = new BranchTargetBufferReadPort(config)
  val write = new BranchTargetBufferWritePort(config)
}

class BranchTargetBufferEntry(config: MusvitConfig) extends Bundle {
  import BranchTargetBuffer.State
  import BranchTargetBuffer.State._
  val target = UInt(ADDR_WIDTH.W)
  val tag    = UInt((ADDR_WIDTH - log2Up(config.btbEntries) - 2).W)
  val valid  = Bool()
  val state  = chiselTypeOf(weakNotTaken)
}

class BranchTargetBuffer(config: MusvitConfig) extends Module {
  val io = IO(new BranchTargetBufferIO(config))
  import BranchTargetBuffer.State
  import BranchTargetBuffer.State._

  def nextState(state: Type, taken: Bool) = {
    MuxCase(weakNotTaken, Seq(
      (state === weakNotTaken && taken)    -> (weakTaken),
      (state === weakNotTaken && !taken)   -> (strongNotTaken),
      (state === strongNotTaken && taken)  -> (weakNotTaken),
      (state === strongNotTaken && !taken) -> (strongNotTaken),
      (state === weakTaken && taken)       -> (strongTaken),
      (state === weakTaken && !taken)      -> (weakNotTaken),
      (state === strongTaken && taken)     -> (strongTaken),
      (state === strongTaken && !taken)    -> (weakTaken),
    ))
  }

  def pcToBTBAddr(pc: UInt): UInt = {
    pc.head(ADDR_WIDTH - 2).tail(ADDR_WIDTH - log2Up(config.btbEntries) - 2)
  }

  def pcToBTBTag(pc: UInt): UInt = {
    pc.head(ADDR_WIDTH - log2Up(config.btbEntries) - 2)
  }

  val btb = RegInit(VecInit(Seq.fill(config.btbEntries)((new BranchTargetBufferEntry(config)).Lit(_.valid -> false.B, _.state -> weakNotTaken))))
  val pcs = Seq.tabulate(config.issueWidth)( (i) => io.read.pc + (i * 4).U)

  def pcToBTBEntry(pc: UInt): BranchTargetBufferEntry = {
    btb(pcToBTBAddr(pc))
  }

  val nextPCArb = Module(new Arbiter(UInt(ADDR_WIDTH.W), config.issueWidth))
  nextPCArb.io.out.ready := true.B
  io.read.chosen := nextPCArb.io.chosen
  
  // Look for matches
  for (i <- 0 until config.issueWidth) {
    nextPCArb.io.in(i).valid := pcToBTBEntry(pcs(i)).valid &&
    pcToBTBEntry(pcs(i)).tag === pcToBTBTag(pcs(i)) &&
    (pcToBTBEntry(pcs(i)).state === strongTaken || pcToBTBEntry(pcs(i)).state === weakTaken)
    nextPCArb.io.in(i).bits := pcToBTBEntry(pcs(i)).target
  }

  io.read.target := DecoupledToValid(nextPCArb.io.out)

  when(io.write.en) {
    btb(pcToBTBAddr(io.write.pc)).state := nextState(btb(pcToBTBAddr(io.write.pc)).state, !io.write.taken)
    when(!io.write.taken) {
      btb(pcToBTBAddr(io.write.pc)).tag := pcToBTBTag(io.write.pc)
      btb(pcToBTBAddr(io.write.pc)).target := io.write.target
      btb(pcToBTBAddr(io.write.pc)).valid := true.B
    }
  }

}