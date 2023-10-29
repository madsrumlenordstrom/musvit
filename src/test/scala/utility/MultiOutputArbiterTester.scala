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
        val inputData = dataSeq.zip(validSeq)
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

      randomTest()
      randomTest()
      randomTest()
      randomTest()
      randomTest()

    }
  }
}
