package utility

import chisel3._
import chisel3.util._
import java.nio.file.Files
import java.io.File
import Constants._

object Functions {
  def fileToBigIntBytes(path: String): Seq[BigInt] = {
    Files
      .readAllBytes((new File(path)).toPath())
      .map(_ & 0xff)
      .map(BigInt(_))
      .toSeq
  }

  def fileToUInts(path: String, width: Int): Seq[UInt] = {
    if (width % BYTE_WIDTH != 0) throw new Error("width must be a multiple of " + BYTE_WIDTH)
    fileToBigIntBytes(path).iterator
      .grouped(width / BYTE_WIDTH)
      .withPadding(BigInt(0))
      .map(
        _.zipWithIndex
          .map { case (lort, i) => (lort << (i * BYTE_WIDTH)) }
          .reduce(_ + _)
          .asUInt(width.W)
      )
      .toSeq
  }

  def bitWidthToUIntMax(width: Int) = ((1L << width.toLong) - 1L)

  def uintToHexString(uint: UInt): String = {
    "%0".concat(((uint.getWidth / BYTE_WIDTH) * 2).toString()).concat("X").format(uint.litValue)
  }
}

object RisingEdge {
  def apply[T <: Data](signal: T): T = {
    (signal.asUInt & ~RegNext(signal).asUInt).asTypeOf(signal)
  }
}

object BitsToByteVec {
  def apply[T <: Bits](data: T): Vec[UInt] = {
    require(data.getWidth % BYTE_WIDTH == 0)
    VecInit(data.asBools.grouped(BYTE_WIDTH).map(VecInit(_).asUInt).toSeq)
  }
}

object SignExtend {
  def apply[T <: Bits](data: T, signBit: Int, extendWidth: Int) = {
    Fill(extendWidth - signBit + 1, data(signBit)) ## data(signBit, 0)
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
