package musvit.fetch


import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants._
import utility.Functions._
import musvit.MusvitConfig
import utility.TestingFunctions._
import scala.util.Random

class BranchTargetBufferTester extends AnyFlatSpec with ChiselScalatestTester {

  // Test configuration
  val config = MusvitConfig.default
  val maxPC = 0xFF

  "BranchTargetBuffer" should "pass" in {
    test(new BranchTargetBuffer(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      def writeBTB(pc: Int, target: Int, taken: Boolean) = {
        dut.io.write.pc.poke(intToUInt(pc))
        dut.io.write.target.poke(intToUInt(target))
        dut.io.write.en.poke(true.B)
        dut.io.write.taken.poke(taken.B)
        dut.clock.step(1)
        dut.io.write.en.poke(false.B)
      }

      for (i <- 0 until config.btbEntries) {
        writeBTB(Random.nextInt(maxPC / 4) * 4, Random.nextInt(maxPC / 4) * 4, true)
      }

      for(i <- 0 until maxPC by (config.issueWidth * 4)) {
        dut.io.read.pc.poke(i.U)
        dut.clock.step(1)
      }
    }
  }
}