package memory

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants._

class ROMTester extends AnyFlatSpec with ChiselScalatestTester {
  val testFile = "sw/build/blinky.bin"
  val width = INST_WIDTH * 2
  "ROM" should "pass" in {
    test(new ROM(width, testFile)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.readEn.poke(true.B)
      for (i <- 0 until dut.rom.length) {
        dut.io.readAddr.poke(i.U)
        dut.clock.step(1)
        println("0x" + i.toHexString + " " +  dut.io.readData.peekInt().toString(16))
      }
    }
  }
}
