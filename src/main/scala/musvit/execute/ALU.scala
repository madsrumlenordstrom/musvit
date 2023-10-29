package musvit.execute

import chisel3._
import chisel3.util._

import musvit.execute.FunctionalUnit
import musvit.MusvitConfig

class ALU (config: MusvitConfig) extends FunctionalUnit(config) {
  val shamt = op2(4, 0)
  
  val lt = op1.asSInt < op2.asSInt
  val ltu = op1 < op2
  val eq = op1 === op2

  io.cdb.bits := MuxCase(DontCare, Seq(
    (op === ALU.ADD)  -> ((op1.asSInt + op2.asSInt).asUInt),
    (op === ALU.SUB)  -> ((op1.asSInt - op2.asSInt).asUInt),
    (op === ALU.AND)  -> (op1 & op2),
    (op === ALU.OR)   -> (op1 | op2),
    (op === ALU.XOR)  -> (op1 ^ op2),
    (op === ALU.SLT)  -> (lt),
    (op === ALU.SLL)  -> (op1 << shamt),
    (op === ALU.SLTU) -> (ltu),
    (op === ALU.SRL)  -> (op1 >> shamt),
    (op === ALU.SRA)  -> ((op1.asSInt >> shamt).asUInt),
  ))

  io.cdb.valid := valid
  ready := io.cdb.ready
}