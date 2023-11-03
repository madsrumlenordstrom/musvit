package utility

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

import utility.Functions._
import utility.Constants._

object TestingFunctions {
  def getRandomData(width: Int): UInt = {
    Random.nextLong(bitWidthToUIntMax(width)).U
  }

  def getRandomWord(): UInt = {
    getRandomData(WORD_WIDTH)
  }

  def intToUInt(data: Int): UInt = {
    ("b" + data.toBinaryString).U
  }
}
