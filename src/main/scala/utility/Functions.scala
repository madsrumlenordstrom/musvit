package utility

import chisel3._
import java.nio.file.Files
import java.io.File

object Functions {
  def fileToByteSeq(file: File) = Files.readAllBytes(file.toPath()).map(_ & 0xff).map(_.asUInt(8.W)).toSeq

  def bitWidthToUIntMax(width: Int) = ((1L << width.toLong) - 1L)
}

object RisingEdge {
  def apply[T <: Data](signal: T): T = {
    (signal.asUInt & ~RegNext(signal).asUInt).asTypeOf(signal)
  }
}
