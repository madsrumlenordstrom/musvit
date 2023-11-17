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
import musvit.common.ControlValues

class MultiplierTester extends FunctionalUnitTester {

  "Multiplier" should "pass" in {
    test(new Multiplier(config))
      .withAnnotations(annotations) { dut =>
        dut.clock.setTimeout(50)

        def mul(data1: Int, data2: Int): Int = { (data1.toLong * data2.toLong & 0x00000000ffffffffL).toInt }

        def mulh(data1: Int, data2: Int): Int = { (data1.toLong * data2.toLong >> 32).toInt }

        def mulhsu(data1: Int, data2: Int): Int = { (data1.toLong * (data2.toLong & 0xffffffffL) >> 32).toInt }

        def mulhu(data1: Int, data2: Int): Int = { ((data1.toLong & 0xffffffffL) * (data2.toLong & 0xffffffffL) >> 32).toInt }

        def randomTest(): Unit = {
          println("Testing with random input")
          for (i <- 0 until iterations) {
            issueExpectFromFunction(dut, MDU.MUL.value.toInt, Random.nextInt(), Random.nextInt(), func = mul, target = 0)
            issueExpectFromFunction(dut, MDU.MULH.value.toInt, Random.nextInt(), Random.nextInt(), func = mulh, target = 0)
            issueExpectFromFunction(dut, MDU.MULHSU.value.toInt, Random.nextInt(), Random.nextInt(), func = mulhsu, target = 0)
            issueExpectFromFunction(dut, MDU.MULHU.value.toInt, Random.nextInt(), Random.nextInt(), func = mulhu, target = 0)
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              issueExpectFromFunction(dut, MDU.MUL.value.toInt, edgeVals(i), edgeVals(j), func = mul, target = 0)
              issueExpectFromFunction(dut, MDU.MULH.value.toInt, edgeVals(i), edgeVals(j), func = mulh, target = 0)
              issueExpectFromFunction(dut, MDU.MULHSU.value.toInt, edgeVals(i), edgeVals(j), func = mulhsu, target = 0)
              issueExpectFromFunction(dut, MDU.MULHU.value.toInt, edgeVals(i), edgeVals(j), func = mulhu, target = 0)
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
    test(new Divider(config))
      .withAnnotations(annotations) { dut =>
        dut.clock.setTimeout(0)

        def div(data1: Int, data2: Int): Int = { data1 / data2 }

        def divu(data1: Int, data2: Int): Int = { ((data1.toLong & 0xffffffffL) / (data2.toLong & 0xffffffffL)).toInt }

        def rem(data1: Int, data2: Int): Int = { data1 % data2 }

        def remu(data1: Int, data2: Int): Int = { ((data1.toLong & 0xffffffffL) % (data2.toLong & 0xffffffffL)).toInt }

        def randomTest(): Unit = {
          println("Testing with random input")
          for (i <- 0 until iterations) {
            issueExpectFromFunction(dut, MDU.DIV.value.toInt, Random.nextInt(), Random.nextInt(), func = div, target = 0)
            issueExpectFromFunction(dut, MDU.DIVU.value.toInt, Random.nextInt(), Random.nextInt(), func = divu, target = 0)
            issueExpectFromFunction(dut, MDU.REM.value.toInt, Random.nextInt(), Random.nextInt(), func = rem, target = 0)
            issueExpectFromFunction(dut, MDU.REMU.value.toInt, Random.nextInt(), Random.nextInt(), func = remu, target = 0)
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              issueExpectFromFunction(dut, MDU.DIV.value.toInt, edgeVals(i), edgeVals(j), func = div, target = 0)
              issueExpectFromFunction(dut, MDU.DIVU.value.toInt, edgeVals(i), edgeVals(j), func = divu, target = 0)
              issueExpectFromFunction(dut, MDU.REM.value.toInt, edgeVals(i), edgeVals(j), func = rem, target = 0)
              issueExpectFromFunction(dut, MDU.REMU.value.toInt, edgeVals(i), edgeVals(j), func = remu, target = 0)
            }
          }
        }

        randomTest()

        edgeCases()

        println("Total steps was " + steps)
      }
  }
}
