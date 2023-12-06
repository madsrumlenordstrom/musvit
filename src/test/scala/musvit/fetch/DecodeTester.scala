package musvit.fetch


import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants._
import utility.Functions._
import musvit.MusvitConfig

class DecodeTester extends AnyFlatSpec with ChiselScalatestTester {

  // Test configuration
  val testFile = "random"
  val width = INST_WIDTH
  val words = fileToUInts(testFile, width)
  val config = MusvitConfig.default

  "Decode" should "pass" in {
    test(new Decode(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      for(i <- 0 until words.length) {
        dut.io.inst.poke(words(i))
        dut.clock.step(1)
      }
    }
  }
}