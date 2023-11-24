package musvit.execute

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import musvit.MusvitConfig
import musvit.fetch.ProgramCounterWritePort
import musvit.fetch.MicroOperationPacket
import musvit.common.ControlValues
import utility.Constants._
import utility.MultiOutputArbiter
import utility.DecoupledToValid
import utility.FunctionalUnitArbiter
import utility.ValidateData

class BackendIO(config: MusvitConfig) extends Bundle {
  val pc    = Output(new ProgramCounterWritePort())
  val flush = Output(Bool())
  val mop   = Flipped(Decoupled(new MicroOperationPacket(config)))
  // needs memIO
}

class Backend(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new BackendIO(config))

  // Pipeline register
  val mopReg = RegEnable(io.mop.bits, 0.U.asTypeOf(io.mop.bits), io.mop.fire)
  val mopRegValid = RegInit(false.B)
  
  // Common data busses
  val cdbs = Seq.fill(config.fetchWidth)(Wire(Valid(CommonDataBus(config))))
  
  val oprSup = Module(new OperandSupplier(config))
  io.pc <> oprSup.io.pc
  io.flush <> oprSup.io.flush
  oprSup.io.cdb <> VecInit(cdbs)
  
  // Values for issuing
  val rds   = Seq.tabulate(config.fetchWidth)( (i) => mopReg.microOps(i).inst(RD_MSB, RD_LSB))
  val rs1s  = Seq.tabulate(config.fetchWidth)( (i) => mopReg.microOps(i).inst(RS1_MSB, RS1_LSB))
  val rs2s  = Seq.tabulate(config.fetchWidth)( (i) => mopReg.microOps(i).inst(RS2_MSB, RS2_LSB))
  val pcs   = Seq.tabulate(config.fetchWidth)( (i) => mopReg.pc + (i * 4).U)
  val ibs   = Seq.fill(config.fetchWidth)(Wire(IssueBus(config)))
  
  // Immediate modules
  val immGens = Seq.fill(config.fetchWidth)(Module(new ImmediateGenerator()))
  
  // Some values for keeping track of issuing
  val fuReady       = Seq.fill(config.fetchWidth)(Wire(Bool()))
  val canIssue      = Seq.fill(config.fetchWidth)(Wire(Bool()))
  val hasIssued     = Seq.fill(config.fetchWidth)(Reg(Bool()))
  val allIssued     = (VecInit(canIssue).asUInt | VecInit(hasIssued).asUInt).andR || !mopRegValid
  io.mop.ready := allIssued

  mopRegValid := Mux(io.mop.fire, true.B, !allIssued)

  // Must be in same order as in fusArbs
  val fus: Seq[Seq[FunctionalUnit]] = Seq(
    Seq.fill(config.aluNum)(Module(new ALU(config))),
    Seq.fill(config.mulNum)(Module(new Multiplier(config))),
    Seq.fill(config.divNum)(Module(new Divider(config))),
    // Seq.fill(config.lsuNum)(Module(new LSU(config))), TODO
  )

  // Arbiters for functional units (must also be in same order as fus)
  val fusArbs: Seq[FunctionalUnitArbiter[IssueBus]] = Seq(
    Module(new FunctionalUnitArbiter(IssueBus(config), config.fetchWidth, config.aluNum)),
    Module(new FunctionalUnitArbiter(IssueBus(config), config.fetchWidth, config.mulNum)),
    Module(new FunctionalUnitArbiter(IssueBus(config), config.fetchWidth, config.divNum)),
    //Module(new FunctionalUnitArbiter(IssueBus(config), config.fetchWidth, config.lsuNum)),
  )

  // Create CDB arbiter
  val fusSeq = fus.reduce(_ ++ _)
  val cdbArb = Module(new MultiOutputArbiter(CommonDataBus(config), fusSeq.length, config.fetchWidth))
  for (i <- 0 until fusSeq.length) { cdbArb.io.in(i) <> fusSeq(i).fu.result } // Input
  for (i <- 0 until config.fetchWidth) { // Output
    cdbs(i) <> DecoupledToValid(cdbArb.io.out(i))
    cdbArb.io.out(i).ready := true.B
  }


  for (i <- 0 until fus.length) {
    for (j <- 0 until fus(i).length) {
      fus(i)(j).rs.flush := oprSup.io.flush
      fus(i)(j).rs.ib <> fusArbs(i).io.out(j)
    }
  }

  // Issue instructions to ROB
  //oprSup.io.issue.valid := mopRegValid && (VecInit(canIssue).asUInt & VecInit(hasIssued).asUInt.unary_~).asBools.reduce(_ || _)
  oprSup.io.issue.valid := mopRegValid && canIssue.zip(hasIssued).map{ case (can, has) => can || !has}.reduce(_ || _)

  // Issue 
  for (i <- 0 until config.fetchWidth) {
    // Check if instruction has funcitonal unit assigned or previously assigned one
    fuReady(i) := fusArbs.map(_.io.in(i).fire).reduce(_ || _) || hasIssued(i)

    // Check if other instructions in packet can not issue
    canIssue(i) := mopRegValid && fuReady.dropRight(config.fetchWidth - 1 - i).reduce(_ && _) // && oprSup.io.issue.ready TODO

    // Mark instructions as issued if not all instructions can issue
    hasIssued(i) := Mux(allIssued, false.B, canIssue(i))

    // Send data to functional units
    for (j <- 0 until fusArbs.length) {
      fusArbs(j).io.in(i).bits := ibs(i)
      fusArbs(j).io.in(i).valid := mopRegValid && mopReg.microOps(i).ctrl.valid && mopReg.microOps(i).ctrl.fu === fus(j).head.fuType && !hasIssued(i)
    }

    // Immediate generation
    immGens(i).io.inst := mopReg.microOps(i).inst
    immGens(i).io.immType := mopReg.microOps(i).ctrl.immType

    // Assign operand supplier values
    oprSup.io.issue.bits(i).bits.branched := mopReg.microOps(i).branched
    oprSup.io.issue.bits(i).bits.data := 0.U // Maybe change to DontCare??
    oprSup.io.issue.bits(i).bits.target := 0.U // Maybe change to DontCare??
    oprSup.io.issue.bits(i).bits.rd := rds(i)
    oprSup.io.issue.bits(i).bits.wb := mopReg.microOps(i).ctrl.wb
    oprSup.io.issue.bits(i).valid := mopReg.microOps(i).ctrl.valid && canIssue(i) && !hasIssued(i)
    oprSup.io.read(i).rs1 := rs1s(i)
    oprSup.io.read(i).rs2 := rs2s(i)

    val pc = Wire(new IssueSource(config))
    pc.robTag := 0.U // Maybe DontCare
    pc.data := ValidateData(pcs(i))
    
    val imm = Wire(new IssueSource(config))
    imm.robTag := 0.U // Maybe DontCare
    imm.data := ValidateData(immGens(i).io.imm)

    val zero = Wire(new IssueSource(config))
    zero.robTag := 0.U // Maybe DontCare
    zero.data := ValidateData(0.U(WORD_WIDTH.W))
    
    // Assign issue bus values
    ibs(i).op := mopReg.microOps(i).ctrl.op
    ibs(i).src1 := MuxCase(oprSup.io.read(i).src1, Seq(
      (mopReg.microOps(i).ctrl.op1 === OP1.PC) -> pc,
      (mopReg.microOps(i).ctrl.op1 === OP1.ZERO) -> zero,
    ))
    ibs(i).src2 := MuxCase(oprSup.io.read(i).src2, Seq(
      (mopReg.microOps(i).ctrl.op2 === OP2.IMM) -> imm,
    ))
    ibs(i).robTag := oprSup.io.read(i).robTag
    ibs(i).imm := Mux(mopReg.microOps(i).branched, 4.U, immGens(i).io.imm)
    ibs(i).pc := pcs(i)

    // Connect functional units
    for (j <- 0 until fus.length) {
      for (k <- 0 until fus(j).length) {
        fus(j)(k).rs.cdb(i) := cdbs(i)
      }
    }
  }
}

object Backend {
  def apply(config: MusvitConfig) = {
    Module(new Backend(config))
  }
}