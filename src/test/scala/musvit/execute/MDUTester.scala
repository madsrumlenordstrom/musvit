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

class MultiplierTester extends FunctionalUnitTester {

  "Multiplier" should "pass" in {
    test(new Multiplier(config, defaultTag))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(50)

        def mul(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.MUL.value.toInt, data1, data2, expected = (data1.toLong * data2.toLong & 0x00000000ffffffffL).toInt)
        }

        def mulh(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.MULH.value.toInt, data1, data2, expected = (data1.toLong * data2.toLong >> 32).toInt)
        }

        def mulhsu(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.MULHSU.value.toInt, data1, data2, expected = (data1.toLong * (data2.toLong & 0xffffffffL) >> 32).toInt)
        }

        def mulhu(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.MULHU.value.toInt, data1, data2,  expected =((data1.toLong & 0xffffffffL) * (data2.toLong & 0xffffffffL) >> 32).toInt)
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

class DividerTester extends FunctionalUnitTester {
  "Divider" should "pass" in {
    test(new Divider(config, defaultTag))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        def div(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.DIV.value.toInt, data1, data2, expected = data1 / data2)
        }

        def divu(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.DIVU.value.toInt, data1, data2, expected = ((data1.toLong & 0xffffffffL) / (data2.toLong & 0xffffffffL)).toInt)
        }

        def rem(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.REM.value.toInt, data1, data2, expected = data1 % data2)
        }

        def remu(data1: Int, data2: Int): Unit = {
          issueExpect(dut, MDU.REMU.value.toInt, data1, data2, expected = ((data1.toLong & 0xffffffffL) % (data2.toLong & 0xffffffffL)).toInt)
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
