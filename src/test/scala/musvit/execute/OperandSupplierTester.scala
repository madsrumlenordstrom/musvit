package musvit.execute

import chisel3._
import chisel3.util._
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
    issueWidth = 4,
    robEntries = 32,
  )

  "OperandSupplier" should "pass" in {
    test(new OperandSupplier(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      val issues = Queue[Seq[ReorderBufferIssueFields]]()
      val writeData = Seq.fill(config.robEntries)(Random.nextInt())
      val writeTargets = Seq.fill(config.robEntries)(Random.nextInt())

      def issueBus(rd: Int, wb: Int, branched: Boolean, valid: Boolean): ReorderBufferIssueFields = {
        chiselTypeOf(dut.io.issue.bits.fields.head).Lit(
          _.rd -> intToUInt(rd),
          _.wb -> intToUInt(wb),
          _.branched -> branched.B,
          _.valid -> valid.B,
        )
      }

      def issueSource(robTag: Int, data: Int, valid: Boolean): IssueSource = {
        chiselTypeOf(dut.io.read.head.src1).Lit(
          _.robTag -> intToUInt(robTag),
          _.data.bits -> intToUInt(data),
          _.data.valid -> valid.B
        )
      }

      def issue(data: Seq[ReorderBufferIssueFields]): Unit = {
        dut.io.issue.valid.poke(true.B)
        for (i <- 0 until dut.io.issue.bits.fields.length) {
          dut.io.issue.bits.fields(i).poke(data(i))
        }
        dut.clock.step(1)
        dut.io.issue.valid.poke(false.B)
      }

      def read(rs: Int, expect: IssueSource): Unit = {
        dut.io.read(0).rs1.poke(intToUInt(rs))
        if (dut.io.read(0).src1.data.valid.peekBoolean()) {
          dut.io.read(0).src1.data.bits.expect(expect.data.bits)
        } else {
          dut.io.read(0).src1.robTag.expect(expect.robTag)
        }
        dut.io.read(0).rs2.poke(intToUInt(rs))
        if (dut.io.read(0).src2.data.valid.peekBoolean()) {
          dut.io.read(0).src2.data.bits.expect(expect.data.bits)
        } else {
          dut.io.read(0).src2.robTag.expect(expect.robTag)
        }
      }

      def write(robTag: Int, data: Int, target: Int): Unit = {
        dut.io.cdb(0).valid.poke(true.B)
        dut.io.cdb(0).bits.robTag.poke(intToUInt(robTag))
        dut.io.cdb(0).bits.data.poke(intToUInt(data))
        dut.io.cdb(0).bits.target.poke(intToUInt(target))
        dut.clock.step(1)
        dut.io.cdb(0).valid.poke(false.B)
      }

      // Issue      
      for (i <- 0 until config.robEntries / config.issueWidth) {
        val data = Seq.tabulate(config.issueWidth)(j => issueBus((config.issueWidth * i) + j, WB.REG.value.toInt, false, true))
        issue(data)
        issues.enqueue(data)
      }

      // Read operands
      for (i <- 0 until config.robEntries) {
        val source = issueSource(i, 0, false)
        read(i, source)
        dut.clock.step(1)
      }

      // Write operands
      for (i <- 0 until config.robEntries) {
        val source = issueSource(i, if (i == 0) 0 else writeData(i), true)
        write(i, writeData(i), writeTargets(i))
        read(i, source)
        dut.clock.step(1)
      }
      dut.clock.step(1)

      // Test branch
      val nonBranched = Seq.fill(config.issueWidth)(issueBus(0, WB.PC.value.toInt, false, true))
      issue(nonBranched)
      dut.io.branch.en.expect(false.B)
      for (i <- 0 until config.issueWidth) {
        write(i, i % 2, writeTargets(i))
      }
      dut.clock.step(1)
      dut.io.branch.en.expect(false.B)

      val branched = Seq.fill(config.issueWidth)(issueBus(0, WB.PC.value.toInt, true, true))
      issue(branched)
      dut.io.branch.en.expect(false.B)
      for (i <- 0 until config.issueWidth) {
        write(i, i % 2, writeTargets(i))
      }
      dut.clock.step(1)
      dut.io.branch.en.expect(false.B)
    }
  }
}
