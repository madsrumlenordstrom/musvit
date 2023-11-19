package musvit.execute

import chisel3._
import chisel3.util._

import utility.Constants._
import musvit.MusvitConfig
import musvit.common.ControlValues
import musvit.execute.IssueSource
import musvit.fetch.ProgramCounterWritePort

class OperandSupplierReadPort(config: MusvitConfig) extends Bundle {
  val rs1   = Input(UInt(REG_ADDR_WIDTH.W))
  val rs2   = Input(UInt(REG_ADDR_WIDTH.W))
  val src1  = new IssueSource(config)
  val src2  = new IssueSource(config)
}

class OperandSupplierIO(config: MusvitConfig) extends Bundle {
  val issue = Flipped(Decoupled(Vec(config.fetchWidth, CommitBus(config))))
  val read  = Vec(config.fetchWidth, new OperandSupplierReadPort(config))
  val cdb   = Vec(config.fetchWidth, Flipped(Valid(CommonDataBus(config))))
  val pc    = Output(new ProgramCounterWritePort())
  val flush = Output(Bool())
}

class OperandSupplier(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new OperandSupplierIO(config))

  val regMap  = Module(new RegisterMapTable(config))
  val rob     = Module(new ReorderBuffer(config))
  val rf      = Module(new RegisterFile(config))
  val pcArb   = Module(new Arbiter(new ProgramCounterWritePort, config.fetchWidth))

  val branches  = Wire(Vec(config.fetchWidth, Bool()))
  val flush     = branches.reduce(_ || _)  && rob.io.commit.fire
  val targets   = Wire(Vec(config.fetchWidth, Decoupled(new ProgramCounterWritePort)))

  io.flush := flush
  regMap.io.flush := flush
  rob.io.flush := flush
  rob.io.issue <> io.issue
  rob.io.cdb <> io.cdb
  rob.io.commit.ready := true.B // TODO figure this out
  pcArb.io.in <> targets
  pcArb.io.out.ready := true.B // TODO figure this out
  io.pc <> pcArb.io.out.bits

  for (i <- 0 until config.fetchWidth) {
    // Check branch
    branches(i) := MuxCase(false.B, Seq(
      (rob.io.commit.bits(i).bits.wb === WB.PC)  -> (rob.io.commit.bits(i).bits.branched ^ rob.io.commit.bits(i).bits.data(0)),
      (rob.io.commit.bits(i).bits.wb === WB.JMP) -> true.B,
    ))

    // Check if a previous instruction in the commit packet has branched or jumped
    val flushed = if (i == 0) false.B else branches.dropRight(config.fetchWidth - i).reduce(_ || _)

    // Connect register map table
    regMap.io.read(i).rs1 := io.read(i).rs1
    regMap.io.read(i).rs2 := io.read(i).rs2
    regMap.io.write(i).rs := io.issue.bits(i).rd
    regMap.io.write(i).en := io.issue.fire && io.issue.bits(i).wb === WB.REG_OR_JMP && !flushed
    regMap.io.write(i).robTag := rob.io.freeTags(i)
    regMap.io.clear(i).rs := rob.io.commit.bits(i).bits.rd
    regMap.io.clear(i).clear := rob.io.commit.fire && rob.io.commit.bits(i).bits.wb === WB.REG_OR_JMP  && !flushed

    // Connect ROB
    rob.io.read(i).robTag1 := regMap.io.read(i).robTag1.bits
    rob.io.read(i).robTag2 := regMap.io.read(i).robTag2.bits

    // Connect register file
    rf.io.read(i).rs1 := io.read(i).rs1
    rf.io.read(i).rs2 := io.read(i).rs2
    rf.io.write(i).rd := rob.io.commit.bits(i).bits.rd
    rf.io.write(i).data := rob.io.commit.bits(i).bits.data
    rf.io.write(i).en := rob.io.commit.fire && rob.io.commit.bits(i).bits.wb === WB.REG_OR_JMP && !flushed

    // Connect targets to PC arbiter
    targets(i).bits.data := rob.io.commit.bits(i).bits.target
    targets(i).bits.en   := branches(i) && rob.io.commit.fire
    targets(i).valid     := branches(i) && rob.io.commit.fire

    def validateData[T <: Data](data: T): Valid[T] = {
      val d = Wire(Valid(chiselTypeOf(data)))
      d.valid := true.B
      d.bits := data
      d
    }

    // Read operand 1
    io.read(i).src1.data := MuxCase(
      validateData(rf.io.read(i).data1),
      Seq.tabulate(io.cdb.length)(j => // CDB bypass
        ((regMap.io.read(i).robTag1.bits === io.cdb(j).bits.robTag) &&
        io.cdb(j).valid &&
        regMap.io.read(i).robTag1.valid)
        -> validateData(io.cdb(j).bits.data)
      ) ++
      Seq((regMap.io.read(i).robTag1.valid) -> rob.io.read(i).data1)
    )

    // Read operand 2
    io.read(i).src2.data := MuxCase(
      validateData(rf.io.read(i).data2),
      Seq.tabulate(io.cdb.length)(j => // CDB bypass
        ((regMap.io.read(i).robTag2.bits === io.cdb(j).bits.robTag) &&
          io.cdb(j).valid &&
          regMap.io.read(i).robTag2.valid) ->
          validateData(io.cdb(j).bits.data)
      ) ++
      Seq((regMap.io.read(i).robTag2.valid) -> rob.io.read(i).data2)
    )

    // Set default ROB tags
    io.read(i).src1.robTag := regMap.io.read(i).robTag1.bits
    io.read(i).src2.robTag := regMap.io.read(i).robTag2.bits

    // Check for conflicting registers and rename
    for (j <- 0 until i) {
      when(
        io.read(i).rs1 === io.issue.bits(j).rd &&
          io.read(i).rs1 =/= 0.U &&
          (io.issue.bits(j).wb === WB.REG_OR_JMP) // JMP should flush so maybe not neccessary
      ) {
        io.read(i).src1.data.valid := false.B
        io.read(i).src1.robTag := regMap.io.read(j).robTag1.bits
      }

      when(
        io.read(i).rs2 === io.issue.bits(j).rd &&
          io.read(i).rs2 =/= 0.U &&
          (io.issue.bits(j).wb === WB.REG_OR_JMP) // JMP should flush so maybe not neccessary
      ) {
        io.read(i).src2.data.valid := false.B
        io.read(i).src2.robTag := regMap.io.read(j).robTag2.bits
      }
    }
  }
}
