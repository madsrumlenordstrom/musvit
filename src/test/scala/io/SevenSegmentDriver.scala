package io

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class BinaryToBCDTester extends AnyFlatSpec with ChiselScalatestTester {

  "BinaryToBCD" should "pass" in {
    test(new BinaryToBCD(16)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)
      def testNumber(n: Int): Unit = {
        dut.io.binary.valid.poke(true.B)
        dut.io.binary.bits.poke(n)
        dut.clock.step(1)
        dut.io.binary.bits.poke(0)
        dut.io.binary.valid.poke(false.B)
        for (i <- 0 until dut.numOfCycles) {
          dut.clock.step(1)
        }
      }
      testNumber(243)
      testNumber(255)
      testNumber(100)
      testNumber(1)
      testNumber(111)
      testNumber(65244)
      testNumber(11111)
      testNumber(0)
      testNumber(737)
    }
  }
}

class MultiplexedSevenSegmentDriverTester extends AnyFlatSpec with ChiselScalatestTester {

  "MultiplexedSevenSegmentDriver" should "pass" in {
    test(new MultiplexedSevenSegmentDriver(4, 10000, false, false, true)).withAnnotations(Seq(WriteVcdAnnotation)) {
      dut =>
        dut.clock.setTimeout(0)
        def testNumber(n: Int): Unit = {
          dut.io.value.poke(n.U)
          for (i <- 0 until 1000) {
            dut.clock.step(1)
          }
        }
        testNumber(243)
        testNumber(255)
        testNumber(100)
        testNumber(1)
        testNumber(111)
        testNumber(65244)
        testNumber(11111)
        testNumber(0)
        testNumber(737)
    }
  }
}
