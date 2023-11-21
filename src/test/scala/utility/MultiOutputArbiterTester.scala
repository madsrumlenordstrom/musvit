package utility

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

class MultiOutputArbiterTester extends AnyFlatSpec with ChiselScalatestTester {
  val inputs = 16
  val outputs = 4
  "MultiOutputArbiter" should "pass" in {
    test(new MultiOutputArbiter(UInt(32.W), inputs, outputs)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      def setInput(data: UInt, valid: Bool, idx: Int): Unit = {
        dut.io.in(idx).bits.poke(data)
        dut.io.in(idx).valid.poke(valid)
      }

      def readOutput(expected: UInt, valid: Bool, idx: Int): Unit = {
        dut.io.out(idx).bits.expect(expected)
        dut.io.out(idx).valid.expect(valid)
      }

      def randomTest(): Unit = {
        val validSeq = Seq.fill(inputs)(Random.nextBoolean().B)
        val dataSeq = Seq.fill(inputs)(Random.nextInt(2147483647).U)
        for (i <- 0 until inputs) {
          setInput(dataSeq(i), validSeq(i), i)
        }
        dut.clock.step(1)

        var count = 0
        for (i <- 0 until inputs) {
          if (validSeq(i).litToBoolean && count < outputs) {
            readOutput(dataSeq(i), validSeq(i), count)
            count += 1
          }
        } 
      }

      for (i <- 0 until outputs) {
        dut.io.out(i).ready.poke(true.B)
      }

      for (i <- 0 until outputs) {
        dut.io.out(i).valid.expect(false.B)
      }

      for (i <- 0 until 100) {
        randomTest()
      }
    }
  }
}


class FunctionalUnitArbiterTester extends AnyFlatSpec with ChiselScalatestTester {
  val inputs = 4
  val outputs = 8
  "FunctionalUnitArbiter" should "pass" in {
    test(new FunctionalUnitArbiter(UInt(32.W), inputs, outputs)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      def setInput(data: UInt, valid: Bool, idx: Int): Unit = {
        dut.io.in(idx).bits.poke(data)
        dut.io.in(idx).valid.poke(valid)
      }

      def readOutput(expected: UInt, valid: Bool, idx: Int): Unit = {
        dut.io.out(idx).bits.expect(expected)
        dut.io.out(idx).valid.expect(valid)
      }

      def setInputValids(valid: Boolean): Unit = {
        for (i <- 0 until inputs) {
          dut.io.in(i).valid.poke(valid.B)
        }
      }

      def setOutputsReadies(ready: Boolean): Unit = {
        for (i <- 0 until outputs) {
          dut.io.out(i).ready.poke(ready.B)
        }
      }

      def randomTest(): Unit = {
        val validSeq = Seq.fill(inputs)(Random.nextBoolean().B)
        val dataSeq = Seq.fill(inputs)(Random.nextInt(2147483647).U)

        setInputValids(false)
        setOutputsReadies(true)

        for (i <- 0 until outputs) {
          dut.io.out(i).valid.expect(false.B)
        }

        for (i <- 0 until inputs) {
          setInput(dataSeq(i), validSeq(i), i)
        }

        dut.clock.step(1)

        var count = 0
        for (i <- 0 until inputs) {
          if (validSeq(i).litToBoolean && count < inputs) {
            readOutput(dataSeq(i), validSeq(i), count)
            count += 1
          }
        }

        setInputValids(false)

        for (i <- 0 until outputs) {
          dut.io.out(i).ready.poke(if (i % 1 == 0) false.B else true.B)
        }

        for (i <- 0 until inputs) {
          dut.io.in(i).valid.poke(true.B)
          dut.io.in(i).bits.poke((i + 7).U)
        }

        dut.clock.step(1)

        for (i <- 0 until outputs) {
          if (i % 1 == 1) {
            dut.io.out(i).valid.expect(true.B)
            dut.io.out(i).bits.expect(((i % 1) + 7).U)
          }
        }
      }

      for (i <- 0 until 100) {
        randomTest()
      }
    }
  }
}