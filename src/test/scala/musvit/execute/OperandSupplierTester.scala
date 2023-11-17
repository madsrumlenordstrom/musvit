package musvit.execute

import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.Queue

import musvit.MusvitConfig
import utility.Functions._
import utility.Constants._
import utility.TestingFunctions._
import musvit.common.ControlValues

class OperandSupplierTester extends AnyFlatSpec with ChiselScalatestTester with ControlValues {
  val config = MusvitConfig(
    fetchWidth = 4,
    robEntries = 32,
  )

  "OperandSupplier" should "pass" in {
    test(new OperandSupplier(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      val issues = Queue[Seq[CommitBus]]()

      def commitBus(data: Int, target: Int, rd: Int, wb: Int): CommitBus = {
        chiselTypeOf(dut.io.issue.bits.head).Lit(
          _.data -> intToUInt(data),
          _.target -> intToUInt(target),
          _.rd -> intToUInt(rd),
          _.wb -> intToUInt(wb),
        )
      }

      def issueSource(robTag: Int, data: Int, valid: Boolean): IssueSource = {
        chiselTypeOf(dut.io.read.head.src1).Lit(
          _.tag -> intToUInt(robTag),
          _.data.bits -> intToUInt(data),
          _.data.valid -> valid.B
        )
      }

      def issue(data: Seq[CommitBus]): Unit = {
        dut.io.issue.valid.poke(true.B)
        for (i <- 0 until dut.io.issue.bits.length) {
          dut.io.issue.bits(i).poke(data(i))
        }
        dut.clock.step(1)
        dut.io.issue.valid.poke(false.B)
      }

      def read(rs: Int, expect: IssueSource): Unit = {
        dut.io.read(0).rs1.poke(intToUInt(rs))
        if (dut.io.read(0).src1.data.valid.peekBoolean()) {
          dut.io.read(0).src1.data.bits.expect(expect.data.bits)
        } else {
          dut.io.read(0).src1.tag.expect(expect.tag)
        }
        dut.io.read(0).rs2.poke(intToUInt(rs))
        if (dut.io.read(0).src2.data.valid.peekBoolean()) {
          dut.io.read(0).src2.data.bits.expect(expect.data.bits)
        } else {
          dut.io.read(0).src2.tag.expect(expect.tag)
        }
      }

      def write(robTag: Int, data: Int): Unit = {
        dut.io.cdb(0).valid.poke(true.B)
        dut.io.cdb(0).bits.tag.poke(intToUInt(robTag))
        dut.io.cdb(0).bits.data.poke(intToUInt(data))
        dut.clock.step(1)
      }

      // Issue      
      for (i <- 0 until config.robEntries / config.fetchWidth) {
        val data = Seq.tabulate(config.fetchWidth)(j => commitBus(Random.nextInt(), Random.nextInt(), (config.fetchWidth * i) + j, WB.REG.value.toInt))
        issue(data)
        issues.enqueue(data)
      }

      // Read operands
      for (i <- 0 until config.robEntries) {
        val data = if (i == 0) 0 else issues(i / config.fetchWidth)(i % config.fetchWidth).data.litValue.toInt
        val valid = if(i == 0) true else false
        val source = issueSource(i, data, valid)
        read(i, source)
        dut.clock.step(1)
      }

      // Write operands
      //for (i <- 0 until config.robEntries) {
      //  val writeVal = Random.nextInt()
      //  write(i, writeVal)
      //  read(i, writeVal, true)
      //  dut.clock.step(1)
      //}

      // Flush
      //dut.io.commit.valid.expect(true.B)
      //dut.io.issue.ready.expect(false.B)
      //dut.io.flush.poke(true.B)
      //dut.clock.step(1)
      //dut.io.commit.valid.expect(false.B)
      //dut.io.issue.ready.expect(true.B)

    }
  }
}
