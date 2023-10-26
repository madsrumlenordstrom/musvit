package musvit.common

import chisel3._
import chisel3.util._

trait OpCodes {
  // Generic yes, no and don't care
  val Y:      BitPat = BitPat("b1")
  val N:      BitPat = BitPat("b0")
  val X:      BitPat = BitPat("b?")

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

    val X:   BitPat = BitPat("b????")
  }
  
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

  object MDU {
    // Maybe set MSB to dont care here?
    def MUL:    BitPat = BitPat("0000")
    def MULH:   BitPat = BitPat("0001")
    def MULHSU: BitPat = BitPat("0010")
    def MULHU:  BitPat = BitPat("0011")
    def DIV:    BitPat = BitPat("0100")
    def DIVU:   BitPat = BitPat("0101")
    def REM:    BitPat = BitPat("0110")
    def REMU:   BitPat = BitPat("0111")

    def X:      BitPat = BitPat("b????")
  }
}