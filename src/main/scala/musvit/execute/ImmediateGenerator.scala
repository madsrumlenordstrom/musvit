package musvit.execute

import chisel3._
import chisel3.util._

import utility.Constants._
import musvit.MusvitConfig
import musvit.common.ControlValues

class ImmediateGeneratorIO extends Bundle with ControlValues {
  val inst = Input(UInt(INST_WIDTH.W))
  val immType = Input(UInt(Imm.X.getWidth.W))
  val imm = Output(UInt(WORD_WIDTH.W))
}

class ImmediateGenerator extends Module with ControlValues {
  val io = IO(new ImmediateGeneratorIO())

  val inst = io.inst
  val immType = io.immType

  io.imm := MuxCase(0.U, Seq(
    (immType === Imm.I) -> (inst(31, 20).asSInt pad WORD_WIDTH).asUInt,
    (immType === Imm.S) -> ((inst(31, 25) ## inst(11, 7)).asSInt pad WORD_WIDTH).asUInt,
    (immType === Imm.B) -> ((inst(31) ## inst(7) ## inst(30, 25) ## inst(11, 8) ## 0.U(1.W)).asSInt pad WORD_WIDTH).asUInt,
    (immType === Imm.U) -> (inst(31, 12) ## 0.U(12.W)),
    (immType === Imm.J) -> ((inst(31) ## inst(19, 12) ## inst(20) ## inst(30, 21) ## 0.U(1.W)).asSInt pad WORD_WIDTH).asUInt,
  ))
}