package utility

import chisel3._
import java.nio.file.Files
import java.io.File
import Constants._

object Functions {
  def fileToByteSeq(path: String): Seq[UInt] = {
    Files
      .readAllBytes((new File(path)).toPath())
      .map(_ & 0xff)
      .map(_.asUInt(BYTE_WIDTH.W))
      .toSeq
  }

  def fileToWordSeq(path: String, width: Int, padding: UInt): Seq[UInt] = {
    if (width % BYTE_WIDTH != 0)
      throw new Error("width must be a multiple of " + BYTE_WIDTH)
    fileToByteSeq(path).iterator
      .grouped(width / BYTE_WIDTH)
      .withPadding(padding)
      .map(_.reduceLeft(_ ## _))
      .toSeq
  }

  def bitWidthToUIntMax(width: Int) = ((1L << width.toLong) - 1L)
}

object RisingEdge {
  def apply[T <: Data](signal: T): T = {
    (signal.asUInt & ~RegNext(signal).asUInt).asTypeOf(signal)
  }
}
