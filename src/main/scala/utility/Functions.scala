package utility

import chisel3._
import chisel3.util._
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

object BarrelShifter {
  private trait ShiftType

  private object LeftShift extends ShiftType

  private object RightShift extends ShiftType

  private object LeftRotate extends ShiftType

  private object RightRotate extends ShiftType

  private def apply[T <: Data](
      inputs: Vec[T],
      shiftInput: UInt,
      shiftType: ShiftType,
      shiftGranularity: Int = 1
  ): Vec[T] = {
    require(shiftGranularity > 0)
    val elementType: T = chiselTypeOf(inputs.head)
    shiftInput.asBools
      .grouped(shiftGranularity)
      .map(VecInit(_).asUInt)
      .zipWithIndex
      .foldLeft(inputs) { case (prev, (shiftBits, layer)) =>
        Mux1H(
          UIntToOH(shiftBits),
          Seq.tabulate(1 << shiftBits.getWidth)(i => {
            val layerShift: Int =
              (i * (1 << (layer * shiftGranularity))).min(prev.length)
            VecInit(shiftType match {
              case LeftRotate =>
                prev.drop(layerShift) ++ prev.take(layerShift)
              case LeftShift =>
                prev.drop(layerShift) ++ Seq
                  .fill(layerShift)(0.U.asTypeOf(elementType))
              case RightRotate =>
                prev.takeRight(layerShift) ++ prev.dropRight(layerShift)
              case RightShift =>
                Seq.fill(layerShift)(0.U.asTypeOf(elementType)) ++ prev
                  .dropRight(layerShift)
            })
          })
        )
      }
  }

  def leftShift[T <: Data](
      inputs: Vec[T],
      shift: UInt,
      layerSize: Int = 1
  ): Vec[T] =
    apply(inputs, shift, LeftShift, layerSize)

  def rightShift[T <: Data](
      inputs: Vec[T],
      shift: UInt,
      layerSize: Int = 1
  ): Vec[T] =
    apply(inputs, shift, RightShift, layerSize)

  def leftRotate[T <: Data](
      inputs: Vec[T],
      shift: UInt,
      layerSize: Int = 1
  ): Vec[T] =
    apply(inputs, shift, LeftRotate, layerSize)

  def rightRotate[T <: Data](
      inputs: Vec[T],
      shift: UInt,
      layerSize: Int = 1
  ): Vec[T] =
    apply(inputs, shift, RightRotate, layerSize)
}
