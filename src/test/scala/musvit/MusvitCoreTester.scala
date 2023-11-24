package musvit

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable.Queue

import musvit.MusvitConfig
import utility.Constants._
import utility.Functions._

class MusvitCoreTester extends AnyFlatSpec with ChiselScalatestTester {
  // Test configuration
  val fetchWidth = 2
  val nop = 0x13.U(WORD_WIDTH.W)
  val testFile = "test.bin"
  val wordsLength = fileToUInts(testFile, INST_WIDTH).length
  val paddings = if (wordsLength % fetchWidth == 0) 0 else wordsLength + (fetchWidth - (wordsLength % fetchWidth))
  val words = fileToUInts(testFile, INST_WIDTH).padTo(paddings, nop)
  val config = MusvitConfig(fetchWidth = fetchWidth, instQueueEntries = words.length, aluNum = 2)

  "MusvitCore" should "pass" in {
    test(new MusvitCore(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      val issues = Queue[Seq[UInt]]()

      def issueInstructions(insts: Seq[UInt]): Unit = {
        dut.io.read.data.valid.poke(true.B)
        for (i <- 0 until insts.length) {
          dut.io.read.data.bits(i).poke(insts(i))
        }
        dut.clock.step(1)
        dut.io.read.data.valid.poke(false.B)
      }

      // Issue instructions
      for (i <- 0 until words.length / config.fetchWidth) {
        while (!dut.io.read.data.ready.peekBoolean()) {
          dut.clock.step(1)
        }
        val addr = dut.io.read.addr.peekInt().toInt / 4
        val insts = Seq.tabulate(config.fetchWidth)( (j) => words(addr + j))
        issueInstructions(insts)
        issues.enqueue(insts)
      }

      // A few extra steps to make sure program completes
      for (i <- 0 until 20) { dut.clock.step(1) }

       
    }
  }
}