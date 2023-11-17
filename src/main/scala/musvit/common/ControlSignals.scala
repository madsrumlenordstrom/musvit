package musvit.common

import chisel3._
import chisel3.util._

import utility.Constants._

trait ControlValues {
  // Generic yes, no and don't care
  def Y:      BitPat = BitPat("b1")
  def N:      BitPat = BitPat("b0")
  def X:      BitPat = BitPat("b?")

  // Width of opcodes
  val OP_WIDTH: Int = 4

  // Memory opcodes
  object Mem {
    def LB:  BitPat = BitPat("b0000")
    def LBU: BitPat = BitPat("b0100")
    def LH:  BitPat = BitPat("b0001")
    def LHU: BitPat = BitPat("b0101")
    def LW:  BitPat = BitPat("b0010")
    def SB:  BitPat = BitPat("b1000")
    def SH:  BitPat = BitPat("b1001")
    def SW:  BitPat = BitPat("b1010")

    def X:   BitPat = BitPat("b????")

    // Not opcodes used for identifying data width
    def BYTE: BitPat = BitPat("b??00")
    def HALF: BitPat = BitPat("b??01")
    def WORD: BitPat = BitPat("b??10")

    // Not opcodes used for indentifying sign
    def SIGNED:   BitPat = BitPat("b?0??")
    def UNSIGNED: BitPat = BitPat("b?1??")

    // Not opcodes used for identifying read/write
    def LOAD:  BitPat = BitPat("b0???")
    def STORE: BitPat = BitPat("b1???")
  }
  
  // ALU opcodes
  object ALU {
    def ADD:    BitPat = BitPat("b0000")
    def SUB:    BitPat = BitPat("b1000")
    def SLL:    BitPat = BitPat("b0001")
    def SLT:    BitPat = BitPat("b0010")
    def SLTU:   BitPat = BitPat("b0011")
    def XOR:    BitPat = BitPat("b0100")
    def SRL:    BitPat = BitPat("b0101")
    def SRA:    BitPat = BitPat("b1101")
    def OR:     BitPat = BitPat("b0110")
    def AND:    BitPat = BitPat("b0111")

    def BEQ:    BitPat = BitPat("b1011")
    def BNE:    BitPat = BitPat("b1001")
    def BLT:    BitPat = BitPat("b0010") // Same as SLT (on purpose)
    def BGE:    BitPat = BitPat("b1010")
    def BLTU:   BitPat = BitPat("b0011") // Same as SLTU (on purpose)
    def BGEU:   BitPat = BitPat("b1111")

    def JAL:    BitPat = BitPat("b1110")
    def JALR:   BitPat = BitPat("b1100")

    // Not opcode but used for identifiying uncondictional jumps
    def JMP:    BitPat = BitPat("b11?0")

    def X:      BitPat = BitPat("b????")
  }

  // MDU opcodes
  object MDU {
    // Maybe set MSB to dont care here?
    def MUL:    BitPat = BitPat("b0000")
    def MULH:   BitPat = BitPat("b0001")
    def MULHSU: BitPat = BitPat("b0010")
    def MULHU:  BitPat = BitPat("b0011")
    def DIV:    BitPat = BitPat("b0100")
    def DIVU:   BitPat = BitPat("b0101")
    def REM:    BitPat = BitPat("b0110")
    def REMU:   BitPat = BitPat("b0111")

    def X:      BitPat = BitPat("b????")
  }

  // Writeback type
  object WB {
    def MEM:  BitPat = BitPat("b00") // Write to memory
    def REG:  BitPat = BitPat("b01") // Write to register file
    def PC:   BitPat = BitPat("b10") // Write to PC
    def JMP:  BitPat = BitPat("b11") // Write both reg and PC
    /* JMP writeback takes whatever is in ROB data and set the PC the value.
    PC+4 which is contained in ROB target is written to RF */

    def REG_OR_JMP: BitPat = BitPat("b?1")
    def X:    BitPat = BitPat("b??")
  }

  // Immediate formats
  object Imm {
    def I: BitPat = BitPat("b000")
    def S: BitPat = BitPat("b001")
    def B: BitPat = BitPat("b010")
    def J: BitPat = BitPat("b011")
    def U: BitPat = BitPat("b100")

    def X: BitPat = BitPat("b???")
  }

  object FU {
    def ALU: BitPat = BitPat("b00")
    def MUL: BitPat = BitPat("b01")
    def DIV: BitPat = BitPat("b10")
    def LSU: BitPat = BitPat("b11")

    def X:   BitPat = BitPat("b??")
  }


  object OP1 {
    def RS1:  BitPat = BitPat("b00")
    def PC:   BitPat = BitPat("b01")
    def ZERO: BitPat = BitPat("b10")

    def X:    BitPat = BitPat("b??")
  }
  
  object OP2 {
    def RS2:  BitPat = BitPat("b00")
    def IMM:  BitPat = BitPat("b01")
    def FOUR: BitPat = BitPat("b10")

    def X:    BitPat = BitPat("b??")
  }
}

class ControlSignals extends Bundle with ControlValues {
  val valid = Bool()
  val op = UInt(OP_WIDTH.W)
  val fu = UInt(FU.X.getWidth.W)
  val op1 = UInt(OP1.X.getWidth.W)
  val op2 = UInt(OP2.X.getWidth.W)
  val immType = UInt(Imm.X.width.W)
  val wb = UInt(WB.X.width.W)
}