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
    val B:  BitPat = BitPat("b01")
    val H:  BitPat = BitPat("b10")
    val W:  BitPat = BitPat("b00")
    val X:  BitPat = BitPat("b??")
  }
}