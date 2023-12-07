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
  val config = MusvitConfig.tiny
  val issueWidth = config.issueWidth
  val nop = 0x13.U(WORD_WIDTH.W)
  val testFile = "sw/build/fibonacci.bin"
  val wordsLength = fileToUInts(testFile, INST_WIDTH).length
  val paddings = if (wordsLength % issueWidth == 0) 0 else wordsLength + (issueWidth - (wordsLength % issueWidth))
  val words = fileToUInts(testFile, INST_WIDTH).padTo(paddings, nop)
  
  var steps = 0
  val maxSteps = 1000

  def step(clk: Clock, n: Int): Unit = {
    clk.step(n)
    steps += n
  }

  def pprint(obj: Any, depth: Int = 0, paramName: Option[String] = None): Unit = {

    val indent = "  " * depth
    val prettyName = paramName.fold("")(x => s"$x: ")
    val ptype = obj match { case _: Iterable[Any] => "" case obj: Product => obj.productPrefix case _ => obj.toString }

    println(s"$indent$prettyName$ptype")

    obj match {
      case seq: Iterable[Any] =>
        seq.foreach(pprint(_, depth + 1))
      case obj: Product =>
        (obj.productIterator zip obj.productElementNames)
          .foreach { case (subObj, paramName) => pprint(subObj, depth + 1, Some(paramName)) }
      case _ =>
    }
  }

  pprint(config)

  "MusvitCore" should "pass" in {
    test(new MusvitCore(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      val issues = Queue[Seq[UInt]]()

      def issueInstructions(insts: Seq[UInt]): Unit = {
        dut.io.read.data.valid.poke(true.B)
        for (i <- 0 until insts.length) {
          dut.io.read.data.bits(i).poke(insts(i))
        }
        step(dut.clock, 1)
        dut.io.read.data.valid.poke(false.B)
      }

      while (!dut.io.exit.peekBoolean() && steps < maxSteps) {
        if (!dut.io.read.data.ready.peekBoolean()) {
          step(dut.clock, 1)
        } else {
          val addr = dut.io.read.addr.peekInt().toInt / 4
          val insts = Seq.tabulate(config.issueWidth)( (j) => words((addr + j) % words.length))
          issueInstructions(insts)
          issues.enqueue(insts)
        }
      }
      
      println("Total steps was " + steps)
       
    }
  }
}