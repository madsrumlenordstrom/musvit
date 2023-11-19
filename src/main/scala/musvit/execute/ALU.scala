package musvit.execute

import chisel3._
import chisel3.util._

import musvit.execute.FunctionalUnit
import musvit.MusvitConfig
import utility.Constants._

class ALU (config: MusvitConfig) extends FunctionalUnit(config) {
  val shamt = data2(4, 0)
  
  val lt = data1.asSInt < data2.asSInt
  val ltu = data1 < data2
  val eq = data1 === data2

  fu.result.bits.data := MuxCase(0.U(WORD_WIDTH.W), Seq(
    (op === ALU.ADD)  -> (data1 + data2),
    (op === ALU.SUB)  -> (data1 - data2),
    (op === ALU.SLL)  -> (data1 << shamt),
    (op === ALU.SLT)  -> (lt),
    (op === ALU.SLTU) -> (ltu),
    (op === ALU.XOR)  -> (data1 ^ data2),
    (op === ALU.SRL)  -> (data1 >> shamt),
    (op === ALU.SRA)  -> ((data1.asSInt >> shamt).asUInt),
    (op === ALU.OR)   -> (data1 | data2),
    (op === ALU.AND)  -> (data1 & data2),
    // Branches
    (op === ALU.BEQ)  -> (eq),
    (op === ALU.BNE)  -> (!eq),
    (op === ALU.BLT)  -> (lt), // Same as SLT (maybe remove)
    (op === ALU.BGE)  -> (!lt || eq),
    (op === ALU.BLTU) -> (ltu), // Same as SLTU (maybe remove)
    (op === ALU.BGEU) -> (!ltu || eq),
    // Jumps
    (op === ALU.JMP)  -> (pc + 4.U),
  ))

  // Calculate jump and branch target
  fu.result.bits.target := Mux(op === ALU.JALR, data1, pc) + imm
    
  fu.result.valid := dataValid && busyReg
}