package musvit.common

import chisel3._
import chisel3.util._

trait OpCodes {
  // Generic yes, no and don't care
  def Y:      BitPat = BitPat("b1")
  def N:      BitPat = BitPat("b0")
  def X:      BitPat = BitPat("b?")
}