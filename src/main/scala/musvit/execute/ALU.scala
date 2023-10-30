package musvit.execute

import chisel3._
import chisel3.util._

import musvit.execute.FunctionalUnit
import musvit.MusvitConfig

class ALU (config: MusvitConfig) extends FunctionalUnit(config) {
  val shamt = data2(4, 0)
  
  val lt = data1.asSInt < data2.asSInt
  val ltu = data1 < data2
  val eq = data1 === data2

  io.cdb.bits := MuxCase(DontCare, Seq(
    (op === ALU.ADD)  -> ((data1.asSInt + data2.asSInt).asUInt),
    (op === ALU.SUB)  -> ((data1.asSInt - data2.asSInt).asUInt),
    (op === ALU.AND)  -> (data1 & data2),
    (op === ALU.OR)   -> (data1 | data2),
    (op === ALU.XOR)  -> (data1 ^ data2),
    (op === ALU.SLT)  -> (lt),
    (op === ALU.SLL)  -> (data1 << shamt),
    (op === ALU.SLTU) -> (ltu),
    (op === ALU.SRL)  -> (data1 >> shamt),
    (op === ALU.SRA)  -> ((data1.asSInt >> shamt).asUInt),
  ))

  io.cdb.valid := valid
  ready := io.cdb.ready
}