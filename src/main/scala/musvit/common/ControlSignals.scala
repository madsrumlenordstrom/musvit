package musvit.common

import chisel3._
import chisel3.util._

trait ControlSignals {
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

  // Reorder buffer type entries
  object ROB {
    def REG:    BitPat = BitPat("b00")
    def STORE:  BitPat = BitPat("b01")
    def BRANCH: BitPat = BitPat("b10")
    def NONE:   BitPat = BitPat("b11")

    def X:      BitPat = BitPat("b??")
  }
}