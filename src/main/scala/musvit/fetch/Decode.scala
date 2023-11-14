package musvit.fetch

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode.TruthTable

import musvit.MusvitConfig
import musvit.common.ControlValues
import utility.Constants._
import musvit.common.RV32I
import musvit.common.RV32M

class DecodeIO(config: MusvitConfig) extends Bundle {
  val inst = Input(UInt(INST_WIDTH.W))
}

class Decode(config: MusvitConfig) extends Module with RV32I with RV32M with ControlValues {
  val io = IO(new DecodeIO(config))

  val defaultCtrl = Seq(N, FU.X, ALU.X, OP1.X, OP2.X, Imm.X, WB.X)

  val table = TruthTable(
    Map(
      //             Valid                  Operand 1
      //             |  Functional unit     |        Operand 2
      //             |  |       Op code     |        |        Immediate type
      //             |  |       |           |        |        |      Writeback type
      //             |  |       |           |        |        |      |
      LUI     -> Seq(Y, FU.ALU, ALU.ADD   , OP1.X  , OP2.IMM, Imm.U, WB.REG), // Figure out op1
      AUIPC   -> Seq(Y, FU.ALU, ALU.ADD   , OP1.PC , OP2.IMM, Imm.U, WB.REG),
      JAL     -> Seq(Y, FU.ALU, ALU.ADD   , OP1.PC , OP2.IMM, Imm.J, WB.JMP),
      JALR    -> Seq(Y, FU.ALU, ALU.ADD   , OP1.RS1, OP2.IMM, Imm.I, WB.JMP),
      BEQ     -> Seq(Y, FU.ALU, ALU.BEQ   , OP1.RS1, OP2.RS2, Imm.B, WB.PC ),
      BNE     -> Seq(Y, FU.ALU, ALU.BNE   , OP1.RS1, OP2.RS2, Imm.B, WB.PC ),
      BLT     -> Seq(Y, FU.ALU, ALU.BLT   , OP1.RS1, OP2.RS2, Imm.B, WB.PC ),
      BGE     -> Seq(Y, FU.ALU, ALU.BGE   , OP1.RS1, OP2.RS2, Imm.B, WB.PC ),
      BLTU    -> Seq(Y, FU.ALU, ALU.BLTU  , OP1.RS1, OP2.RS2, Imm.B, WB.PC ),
      BGEU    -> Seq(Y, FU.ALU, ALU.BGEU  , OP1.RS1, OP2.RS2, Imm.B, WB.PC ),
      LB      -> Seq(Y, FU.LSU, Mem.LB    , OP1.RS1, OP2.X  , Imm.I, WB.REG),
      LH      -> Seq(Y, FU.LSU, Mem.LH    , OP1.RS1, OP2.X  , Imm.I, WB.REG),
      LW      -> Seq(Y, FU.LSU, Mem.LW    , OP1.RS1, OP2.X  , Imm.I, WB.REG),
      LBU     -> Seq(Y, FU.LSU, Mem.LBU   , OP1.RS1, OP2.X  , Imm.I, WB.REG),
      LHU     -> Seq(Y, FU.LSU, Mem.LHU   , OP1.RS1, OP2.X  , Imm.I, WB.REG),
      SB      -> Seq(Y, FU.LSU, Mem.SB    , OP1.RS1, OP2.RS2, Imm.S, WB.MEM),
      SH      -> Seq(Y, FU.LSU, Mem.SH    , OP1.RS1, OP2.RS2, Imm.S, WB.MEM),
      SW      -> Seq(Y, FU.LSU, Mem.SW    , OP1.RS1, OP2.RS2, Imm.S, WB.MEM),
      ADDI    -> Seq(Y, FU.ALU, ALU.ADD   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      SLTI    -> Seq(Y, FU.ALU, ALU.SLT   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      SLTIU   -> Seq(Y, FU.ALU, ALU.SLTU  , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      XORI    -> Seq(Y, FU.ALU, ALU.XOR   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      ORI     -> Seq(Y, FU.ALU, ALU.OR    , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      ANDI    -> Seq(Y, FU.ALU, ALU.AND   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      SLLI    -> Seq(Y, FU.ALU, ALU.SLL   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      SRLI    -> Seq(Y, FU.ALU, ALU.SRL   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      SRAI    -> Seq(Y, FU.ALU, ALU.SRA   , OP1.RS1, OP2.IMM, Imm.I, WB.REG),
      ADD     -> Seq(Y, FU.ALU, ALU.ADD   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      SUB     -> Seq(Y, FU.ALU, ALU.SUB   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      SLL     -> Seq(Y, FU.ALU, ALU.SLL   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      SLT     -> Seq(Y, FU.ALU, ALU.SLT   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      SLTU    -> Seq(Y, FU.ALU, ALU.SLTU  , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      XOR     -> Seq(Y, FU.ALU, ALU.XOR   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      SRL     -> Seq(Y, FU.ALU, ALU.SRL   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      SRA     -> Seq(Y, FU.ALU, ALU.SRA   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      OR      -> Seq(Y, FU.ALU, ALU.OR    , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      AND     -> Seq(Y, FU.ALU, ALU.AND   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      MUL     -> Seq(Y, FU.MUL, MDU.MUL   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      MULH    -> Seq(Y, FU.MUL, MDU.MULH  , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      MULHSU  -> Seq(Y, FU.MUL, MDU.MULHSU, OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      MULHU   -> Seq(Y, FU.MUL, MDU.MULHU , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      DIV     -> Seq(Y, FU.DIV, MDU.DIV   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      DIVU    -> Seq(Y, FU.DIV, MDU.DIVU  , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      REM     -> Seq(Y, FU.DIV, MDU.REM   , OP1.RS1, OP2.RS2, Imm.X, WB.REG),
      REMU    -> Seq(Y, FU.DIV, MDU.REMU  , OP1.RS1, OP2.RS2, Imm.X, WB.REG),

      //FENCE   -> Seq(Y, ALU.ADD),
      //ECALL   -> Seq(Y, ALU.ADD),
      //EBREAK  -> Seq(Y, ALU.ADD),
    ).map({case (k, v) => k -> v.reduce(_ ## _)}),
    defaultCtrl.reduce(_ ## _)
  )
}
