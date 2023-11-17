package musvit.execute

import chisel3._
import chisel3.util._

import utility.Constants._
import musvit.MusvitConfig
import musvit.common.ControlValues
import musvit.execute.IssueSource

class OperandSupplierReadPort(config: MusvitConfig) extends Bundle {
  val rs1 = Input(UInt(REG_ADDR_WIDTH.W))
  val rs2 = Input(UInt(REG_ADDR_WIDTH.W))
  val src1 = new IssueSource(config)
  val src2 = new IssueSource(config)
}

class OperandSupplierIO(config: MusvitConfig) extends Bundle {
  val issue = Flipped(Decoupled(Vec(config.fetchWidth, CommitBus(config))))
  val read = Vec(config.fetchWidth, new OperandSupplierReadPort(config))
  val cdb = Vec(config.fetchWidth, Flipped(Valid(CommonDataBus(config))))
}

class OperandSupplier(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new OperandSupplierIO(config))

  val regMap = Module(new RegisterMapTable(config))
  val rob = Module(new ReorderBuffer(config))
  val rf = Module(new RegisterFile(config))

  val flush = WireDefault(false.B)

  regMap.io.flush := flush
  rob.io.flush := flush
  rob.io.issue <> io.issue
  rob.io.cdb <> io.cdb
  rob.io.commit.ready := true.B // TODO figure this out

  for (i <- 0 until config.fetchWidth) {
    // Connect register map table
    regMap.io.read(i).rs1 := io.read(i).rs1
    regMap.io.read(i).rs2 := io.read(i).rs2
    regMap.io.write(i).rs := io.issue.bits(i).rd
    regMap.io.write(i).en := io.issue.fire && io.issue.bits(i).wb === WB.REG_OR_JMP
    regMap.io.write(i).robTag := rob.io.freeTags(i)  //0.U// io.read(i).src1.tag TODO
    regMap.io.clear(i).rs := rob.io.commit.bits(i).rd
    regMap.io.clear(i).clear := rob.io.commit.fire && rob.io.commit.bits(i).wb === WB.REG_OR_JMP

    // Connect ROB
    rob.io.read(i).robTag1 := regMap.io.read(i).robTag1.bits
    rob.io.read(i).robTag2 := regMap.io.read(i).robTag2.bits

    // Connect register file
    rf.io.read(i).rs1 := io.read(i).rs1
    rf.io.read(i).rs2 := io.read(i).rs2
    rf.io.write(i).rd := rob.io.commit.bits(i).rd
    rf.io.write(i).data := Mux(
      rob.io.commit.bits(i).wb === WB.JMP,
      rob.io.commit.bits(i).target,
      rob.io.commit.bits(i).data
    )
    rf.io.write(i).en := rob.io.commit.fire && rob.io.commit.bits(i).wb === WB.REG_OR_JMP

    def validateData[T <: Data](data: T): Valid[T] = {
      val d = Wire(Valid(chiselTypeOf(data)))
      d.valid := true.B
      d.bits := data
      d
    }

    // Read operand 1
    io.read(i).src1.data := MuxCase(
      validateData(rf.io.read(i).data1),
      Seq((regMap.io.read(i).robTag1.valid) -> rob.io.read(i).data1) ++
      Seq.tabulate(io.cdb.length)(j => // CDB bypass
        ((regMap.io.read(i).robTag1.bits === io.cdb(j).bits.tag) &&
        io.cdb(j).valid &&
        regMap.io.read(i).robTag1.valid)
        -> validateData(io.cdb(j).bits.data)
      )
    )

    // Read operand 2
    io.read(i).src2.data := MuxCase(
      validateData(rf.io.read(i).data2),
      Seq((regMap.io.read(i).robTag2.valid) -> rob.io.read(i).data2) ++
      Seq.tabulate(io.cdb.length)(j => // CDB bypass
        ((regMap.io.read(i).robTag2.bits === io.cdb(j).bits.tag) &&
          io.cdb(j).valid &&
          regMap.io.read(i).robTag2.valid) ->
          validateData(io.cdb(j).bits.data)
      )
    )

    // Set default ROB tags
    io.read(i).src1.tag := regMap.io.read(i).robTag1.bits
    io.read(i).src2.tag := regMap.io.read(i).robTag2.bits

    // Check for conflicting registers and rename
    for (j <- 0 until i) {
      when(
        io.read(i).rs1 === io.issue.bits(j).rd &&
          io.read(i).rs1 =/= 0.U && // JMP should flush so maybe not neccessary
          (io.issue.bits(j).wb === WB.REG_OR_JMP)
      ) {
        io.read(i).src1.data.valid := false.B
        io.read(i).src1.tag := regMap.io.read(j).robTag1.bits
      }

      when(
        io.read(i).rs2 === io.issue.bits(j).rd &&
          io.read(i).rs2 =/= 0.U && // JMP should flush so maybe not neccessary
          (io.issue.bits(j).wb === WB.REG_OR_JMP)
      ) {
        io.read(i).src2.data.valid := false.B
        io.read(i).src2.tag := regMap.io.read(j).robTag2.bits
      }
    }
  }
}
