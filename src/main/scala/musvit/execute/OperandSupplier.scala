package musvit.execute

import chisel3._
import chisel3.util._

import utility.Constants._
import utility.ValidateData
import musvit.MusvitConfig
import musvit.common.ControlValues
import musvit.execute.IssueSource
import musvit.fetch.ProgramCounterWritePort

class OperandSupplierReadPort(config: MusvitConfig) extends Bundle {
  val rs1     = Input(UInt(REG_ADDR_WIDTH.W))
  val rs2     = Input(UInt(REG_ADDR_WIDTH.W))
  val src1    = Output(new IssueSource(config))
  val src2    = Output(new IssueSource(config))
  val robTag  = Output(ROBTag(config)) // ROB tag assigned to instruction and result
}

class OperandSupplierIO(config: MusvitConfig) extends Bundle {
  val issue = Flipped(Decoupled(Vec(config.issueWidth, Valid(CommitBus(config)))))
  val read  = Vec(config.issueWidth, new OperandSupplierReadPort(config))
  val cdb   = Vec(config.issueWidth, Flipped(Valid(CommonDataBus(config))))
  val pc    = Output(new ProgramCounterWritePort())
  val flush = Output(Bool())
  val exit  = Output(Bool())
}

class OperandSupplier(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new OperandSupplierIO(config))

  val regMap  = Module(new RegisterMapTable(config))
  val rob     = Module(new ReorderBuffer(config))
  val rf      = Module(new RegisterFile(config))
  val pcArb   = Module(new Arbiter(new ProgramCounterWritePort(), config.issueWidth))

  val branches      = Seq.fill(config.issueWidth)(Wire(Bool()))
  val isEcall       = Seq.fill(config.issueWidth)(Wire(Bool()))
  val canCommit     = Seq.fill(config.issueWidth)(Wire(Bool()))
  val hasCommitted  = Seq.fill(config.issueWidth)(Reg(Bool()))
  val isValid       = Seq.fill(config.issueWidth)(Wire(Bool()))
  val allCommitted  = (VecInit(canCommit).asUInt | VecInit(hasCommitted).asUInt | VecInit(isValid).asUInt.unary_~).andR || !rob.io.commit.valid
  val flush         = branches.reduce(_ || _) && rob.io.commit.fire

  io.flush := flush
  regMap.io.flush := flush
  rob.io.flush := flush
  rob.io.issue <> io.issue
  rob.io.cdb <> io.cdb
  rob.io.commit.ready := allCommitted
  pcArb.io.out.ready := true.B // TODO figure this out
  io.pc <> pcArb.io.out.bits
  rf.io.ecall := (VecInit(canCommit).asUInt & VecInit(isEcall).asUInt).orR
  io.exit := rf.io.exit

  def getPrevElemsInSeq[T <: Data](seq: Seq[T], i: Int, onZero: T): Seq[T] = {
    if (i == 0) Seq(onZero) else seq.dropRight(config.issueWidth - i)
  }

  for (i <- 0 until config.issueWidth) {
    // Check if a previous instruction in the commit packet has branched or jumped
    val flushed = getPrevElemsInSeq(branches, i, false.B).reduce(_ || _) // if (i == 0) false.B else branches.dropRight(config.issueWidth - i).reduce(_ || _)
    val prevEcall = getPrevElemsInSeq(isEcall, i, false.B).reduce(_ || _)
    val prevCommit = getPrevElemsInSeq(canCommit, i ,false.B).reduce(_ || _)
    val ecallCon = (!prevEcall && !prevCommit) || (!isEcall(i) && !prevEcall)
    isValid(i) := rob.io.commit.bits(i).fire && !hasCommitted(i)
    canCommit(i) := isValid(i) && rob.io.ready(i) && ecallCon && !flushed && (VecInit(getPrevElemsInSeq(canCommit, i, true.B)).asUInt | VecInit(getPrevElemsInSeq(isValid, i, true.B)).asUInt.unary_~).andR
    isEcall(i) := isValid(i) && rob.io.commit.bits(i).bits.wb === WB.ECL

    hasCommitted(i) := Mux(allCommitted, false.B, canCommit(i) || hasCommitted(i))
    
    // Check branch
    branches(i) := MuxCase(false.B, Seq(
      (rob.io.commit.bits(i).bits.wb === WB.PC)  -> (rob.io.commit.bits(i).bits.branched ^ rob.io.commit.bits(i).bits.data(0)),
      (rob.io.commit.bits(i).bits.wb === WB.JMP) -> true.B,
    )) && isValid(i) && canCommit(i)

    // Issue condition
    val issueValid = io.issue.fire && io.issue.bits(i).fire && !flush

    // Connect register map table
    regMap.io.read(i).rs1 := io.read(i).rs1
    regMap.io.read(i).rs2 := io.read(i).rs2
    regMap.io.write(i).rs := io.issue.bits(i).bits.rd
    regMap.io.write(i).en := issueValid && io.issue.bits(i).bits.wb === WB.REG_OR_JMP
    regMap.io.write(i).robTag := rob.io.issueTags(i)
    regMap.io.clear(i).rs := rob.io.commit.bits(i).bits.rd
    regMap.io.clear(i).clear := canCommit(i) && rob.io.commit.bits(i).bits.wb === WB.REG_OR_JMP
    regMap.io.clear(i).robTag := rob.io.commitTags(i)

    // Connect ROB
    rob.io.read(i).robTag1 := regMap.io.read(i).robTag1.bits
    rob.io.read(i).robTag2 := regMap.io.read(i).robTag2.bits

    // Connect register file
    rf.io.read(i).rs1 := io.read(i).rs1
    rf.io.read(i).rs2 := io.read(i).rs2
    rf.io.write(i).rd := rob.io.commit.bits(i).bits.rd
    rf.io.write(i).data := rob.io.commit.bits(i).bits.data
    rf.io.write(i).en := canCommit(i) && rob.io.commit.bits(i).bits.wb === WB.REG_OR_JMP

    // Connect targets to PC arbiter
    pcArb.io.in(i).bits.data  := rob.io.commit.bits(i).bits.target
    pcArb.io.in(i).valid      := (canCommit(i) && branches(i)).asBool
    pcArb.io.in(i).bits.en    := (canCommit(i) && branches(i)).asBool

    // Read operand 1
    io.read(i).src1.data := MuxCase(
      ValidateData(rf.io.read(i).data1),
      Seq.tabulate(io.cdb.length)(j => // CDB bypass
        ((regMap.io.read(i).robTag1.bits === io.cdb(j).bits.robTag) &&
        io.cdb(j).valid &&
        regMap.io.read(i).robTag1.valid)
        -> ValidateData(io.cdb(j).bits.data)
      ) ++
      Seq((regMap.io.read(i).robTag1.valid) -> rob.io.read(i).data1)
    )

    // Read operand 2
    io.read(i).src2.data := MuxCase(
      ValidateData(rf.io.read(i).data2),
      Seq.tabulate(io.cdb.length)(j => // CDB bypass
        ((regMap.io.read(i).robTag2.bits === io.cdb(j).bits.robTag) &&
          io.cdb(j).valid &&
          regMap.io.read(i).robTag2.valid) ->
          ValidateData(io.cdb(j).bits.data)
      ) ++
      Seq((regMap.io.read(i).robTag2.valid) -> rob.io.read(i).data2)
    )

    // Set default ROB tags
    io.read(i).src1.robTag := regMap.io.read(i).robTag1.bits
    io.read(i).src2.robTag := regMap.io.read(i).robTag2.bits
    io.read(i).robTag := rob.io.issueTags(i)

    // Check for conflicting registers and rename
    for (j <- 0 until i) {
      when(
        io.read(i).rs1 === io.issue.bits(j).bits.rd &&
          io.read(i).rs1 =/= 0.U &&
          io.issue.bits(j).bits.wb === WB.REG_OR_JMP && // JMP should flush so maybe not neccessary
          io.issue.bits(j).fire
      ) {
        io.read(i).src1.data.valid := false.B
        io.read(i).src1.robTag := rob.io.issueTags(j)
      }

      when(
        io.read(i).rs2 === io.issue.bits(j).bits.rd &&
          io.read(i).rs2 =/= 0.U &&
          io.issue.bits(j).bits.wb === WB.REG_OR_JMP && // JMP should flush so maybe not neccessary
          io.issue.bits(j).fire
      ) {
        io.read(i).src2.data.valid := false.B
        io.read(i).src2.robTag := rob.io.issueTags(j)
      }
    }
  }
}
