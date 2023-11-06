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
import musvit.common.ControlSignals

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
            issueExpectFromFunction(dut, MDU.MUL.value.toInt, Random.nextInt(), Random.nextInt(), func = mul)
            issueExpectFromFunction(dut, MDU.MULH.value.toInt, Random.nextInt(), Random.nextInt(), func = mulh)
            issueExpectFromFunction(dut, MDU.MULHSU.value.toInt, Random.nextInt(), Random.nextInt(), func = mulhsu)
            issueExpectFromFunction(dut, MDU.MULHU.value.toInt, Random.nextInt(), Random.nextInt(), func = mulhu)
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              issueExpectFromFunction(dut, MDU.MUL.value.toInt, edgeVals(i), edgeVals(j), func = mul)
              issueExpectFromFunction(dut, MDU.MULH.value.toInt, edgeVals(i), edgeVals(j), func = mulh)
              issueExpectFromFunction(dut, MDU.MULHSU.value.toInt, edgeVals(i), edgeVals(j), func = mulhsu)
              issueExpectFromFunction(dut, MDU.MULHU.value.toInt, edgeVals(i), edgeVals(j), func = mulhu)
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
            issueExpectFromFunction(dut, MDU.DIV.value.toInt, Random.nextInt(), Random.nextInt(), func = div)
            issueExpectFromFunction(dut, MDU.DIVU.value.toInt, Random.nextInt(), Random.nextInt(), func = divu)
            issueExpectFromFunction(dut, MDU.REM.value.toInt, Random.nextInt(), Random.nextInt(), func = rem)
            issueExpectFromFunction(dut, MDU.REMU.value.toInt, Random.nextInt(), Random.nextInt(), func = remu)
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              issueExpectFromFunction(dut, MDU.DIV.value.toInt, edgeVals(i), edgeVals(j), func = div)
              issueExpectFromFunction(dut, MDU.DIVU.value.toInt, edgeVals(i), edgeVals(j), func = divu)
              issueExpectFromFunction(dut, MDU.REM.value.toInt, edgeVals(i), edgeVals(j), func = rem)
              issueExpectFromFunction(dut, MDU.REMU.value.toInt, edgeVals(i), edgeVals(j), func = remu)
            }
          }
        }

        randomTest()

        edgeCases()

        println("Total steps was " + steps)
      }
  }
}
