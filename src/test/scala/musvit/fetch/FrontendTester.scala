package musvit.fetch

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable.Queue

import musvit.MusvitConfig
import utility.Constants._
import utility.Functions._

class FrontendTester extends AnyFlatSpec with ChiselScalatestTester {
  // Test configuration
  val testFile = "random"
  val words = fileToUInts(testFile, INST_WIDTH)
  val fetchWidth = 2
  val config = MusvitConfig(fetchWidth = fetchWidth, instQueueEntries = words.length)

  "Frontend" should "pass" in {
    test(new Frontend(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      val issues = Queue[Seq[UInt]]()

      dut.io.pc.en.poke(false.B)

      def issueInstructions(insts: Seq[UInt]): Unit = {
        for (i <- 0 until insts.length) {
          dut.io.read.data(i).poke(insts(i))
        }
      }

      def readInstructions(insts: Seq[UInt]): Unit = {
        dut.io.mop.valid.expect(true.B)
        dut.io.mop.ready.poke(true.B)
        for (i <- 0 until insts.length) {
          dut.io.mop.bits.microOps(i).inst.expect(insts(i))
        }
        dut.clock.step(1)
        dut.io.mop.ready.poke(false.B)
      }

      // Issue instructions
      for (i <- 0 until words.length / config.fetchWidth) {
        val addr = dut.io.read.addr.peekInt().toInt / 4
        val insts = Seq.tabulate(config.fetchWidth)( (j) => words(addr + j))
        issueInstructions(insts)
        issues.enqueue(insts)
        dut.clock.step(1)
      }

      // Read
      for (i <- 0 until words.length / config.fetchWidth) {
        readInstructions(issues.dequeue())
      }
    }
  }
}