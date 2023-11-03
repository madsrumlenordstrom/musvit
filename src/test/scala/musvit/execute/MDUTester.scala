package musvit.execute

import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

import musvit.MusvitConfig
import utility.Functions._
import utility.Constants._
import utility.TestingFunctions._
import musvit.common.OpCodes

class MultiplierTester extends AnyFlatSpec with ChiselScalatestTester with OpCodes {
  val config = MusvitConfig.default

  val iterations = 1000
  var steps = 0

  "Multiplier" should "pass" in {
    test(new Multiplier(config))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        def step(n: Int): Unit = {
          dut.clock.step(n)
          steps += n
        }

        def fuData(op: UInt, data1: UInt, data2: UInt): FunctionalUnitOperands = {
          chiselTypeOf(dut.io.rs.bits).Lit(
            _.op -> op,
            _.data1 -> data1,
            _.data2 -> data2,
            )
        }

        def issue(operands: FunctionalUnitOperands): Unit = {
          dut.io.rs.bits.poke(operands)
          dut.io.rs.valid.poke(true.B)
          step(1)
          dut.io.rs.valid.poke(false.B)
        }

        def read(expected: UInt): Unit = {
          dut.io.result.ready.poke(true.B)
          while (!dut.io.result.valid.peekBoolean()) {
            step(1)
          }
          dut.io.result.bits.expect(expected)
        }

        def mul(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.MUL.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = (data1.toLong * data2.toLong & 0x00000000ffffffffL).toInt
          read(intToUInt(product))
        }

        def mulh(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.MULH.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = (data1.toLong * data2.toLong >> 32).toInt
          read(intToUInt(product))
        }

        def mulhsu(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.MULHSU.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = (data1.toLong * (data2.toLong & 0xffffffffL) >> 32).toInt
          read(intToUInt(product))
        }

        def mulhu(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.MULHU.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = ((data1.toLong & 0xffffffffL) * (data2.toLong & 0xffffffffL) >> 32).toInt
          read(intToUInt(product))
        }

        def randomTest(): Unit = {
          println("Testing with random input")
          for (i <- 0 until iterations) {
            mul(Random.nextInt(), Random.nextInt())
            mulh(Random.nextInt(), Random.nextInt())
            mulhsu(Random.nextInt(), Random.nextInt())
            mulhu(Random.nextInt(), Random.nextInt())
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              mul(edgeVals(i), edgeVals(j))
              mulh(edgeVals(i), edgeVals(j))
              mulhsu(edgeVals(i), edgeVals(j))
              mulhu(edgeVals(i), edgeVals(j))
            }
          }
        }

        randomTest()
        edgeCases()

        println("Total steps was " + steps)
      }
  }
}

class DividerTester extends AnyFlatSpec with ChiselScalatestTester with OpCodes {
  val config = MusvitConfig.default

  val iterations = 1000
  var steps = 0

  "Divider" should "pass" in {
    test(new Divider(config))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        def step(n: Int): Unit = {
          dut.clock.step(n)
          steps += n
        }

        def fuData(op: UInt, data1: UInt, data2: UInt): FunctionalUnitOperands = {
          chiselTypeOf(dut.io.rs.bits).Lit(
            _.op -> op,
            _.data1 -> data1,
            _.data2 -> data2,
            )
        }

        def issue(operands: FunctionalUnitOperands): Unit = {
          dut.io.rs.bits.poke(operands)
          dut.io.rs.valid.poke(true.B)
          step(1)
          dut.io.rs.valid.poke(false.B)
        }

        def read(expected: UInt): Unit = {
          dut.io.result.ready.poke(true.B)
          while (!dut.io.result.valid.peekBoolean()) {
            step(1)
          }
          dut.io.result.bits.expect(expected)
          dut.clock.step(1)
        }

        def div(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.DIV.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 / data2
          read(intToUInt(product))
        }

        def divu(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.DIVU.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = ((data1.toLong & 0xffffffffL) / (data2.toLong & 0xffffffffL)).toInt
          read(intToUInt(product))
        }

        def rem(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.REM.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 % data2
          read(intToUInt(product))
        }

        def remu(data1: Int, data2: Int): Unit = {
          val data = fuData(MDU.REMU.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = ((data1.toLong & 0xffffffffL) % (data2.toLong & 0xffffffffL)).toInt
          read(intToUInt(product))
        }

        def randomTest(): Unit = {
          println("Testing with random input")
          for (i <- 0 until iterations) {
            div(Random.nextInt(), Random.nextInt())
            divu(Random.nextInt(), Random.nextInt())
            rem(Random.nextInt(), Random.nextInt())
            remu(Random.nextInt(), Random.nextInt())
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              div(edgeVals(i), edgeVals(j))
              divu(edgeVals(i), edgeVals(j))
              rem(edgeVals(i), edgeVals(j))
              remu(edgeVals(i), edgeVals(j))
            }
          }
        }

        randomTest()

        edgeCases()

        println("Total steps was " + steps)
      }
  }
}