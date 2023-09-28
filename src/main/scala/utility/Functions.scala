package utility

import chisel3._
import java.nio.file.Files
import java.io.File
import Constants.BYTE_W

object Functions {
  def fileToByteSeq(file: File) = Files.readAllBytes(file.toPath()).map(_ & 0xff).map(_.asUInt(BYTE_W.W)).toSeq

  def bitWidthToUIntMax(width: Int) = ((1L << width.toLong) - 1L)
}

object RisingEdge {
  def apply[T <: Data](signal: T): T = {
    (signal.asUInt & ~RegNext(signal).asUInt).asTypeOf(signal)
  }
}
