package musvit.execute

import chisel3._
import chisel3.util._

import musvit.execute.FunctionalUnit
import musvit.MusvitConfig

class ALU (config: MusvitConfig) extends FunctionalUnit(config) {
  val shamt = opr2(4, 0)
  
  val lt = opr1.asSInt < opr2.asSInt
  val ltu = opr1 < opr2
  val eq = opr1 === opr2

  io.cdb.bits := MuxCase(DontCare, Seq(
    (op === ALU.ADD)  -> ((opr1.asSInt + opr2.asSInt).asUInt),
    (op === ALU.SUB)  -> ((opr1.asSInt - opr2.asSInt).asUInt),
    (op === ALU.AND)  -> (opr1 & opr2),
    (op === ALU.OR)   -> (opr1 | opr2),
    (op === ALU.XOR)  -> (opr1 ^ opr2),
    (op === ALU.SLT)  -> (lt),
    (op === ALU.SLL)  -> (opr1 << shamt),
    (op === ALU.SLTU) -> (ltu),
    (op === ALU.SRL)  -> (opr1 >> shamt),
    (op === ALU.SRA)  -> ((opr1.asSInt >> shamt).asUInt),
  ))

  io.cdb.valid := valid
  ready := io.cdb.ready
}