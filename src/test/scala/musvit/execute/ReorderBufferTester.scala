package musvit.execute

import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.Queue

import musvit.MusvitConfig
import musvit.common.ControlValues
import utility.Functions._
import utility.Constants._
import utility.TestingFunctions._

class ReorderBufferTester extends AnyFlatSpec with ChiselScalatestTester with ControlValues {
  val config = MusvitConfig(
    issueWidth = 2,
    robEntries = 16,
  )

  "ReorderBuffer" should "pass" in {
    test(new ReorderBuffer(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)
      dut.io.flush.poke(false.B)

      val issues = Queue[Seq[Valid[CommitBus]]]()
      val writeData = Seq.fill(config.robEntries)(Random.nextInt())
      val writeTargets = Seq.fill(config.robEntries)(Random.nextInt())

      def reorderBufferEntry(data: Int, target: Int, rd: Int, wb: Int, branched: Boolean, valid: Boolean): Valid[CommitBus] = {
        chiselTypeOf(dut.io.issue.bits.head).Lit(
          _.bits.data -> intToUInt(data),
          _.bits.target -> intToUInt(target),
          _.bits.rd -> intToUInt(rd),
          _.bits.wb -> intToUInt(wb),
          _.bits.branched -> branched.B,
          _.valid -> valid.B
        )
      }

      def issue(data: Seq[Valid[CommitBus]]): Unit = {
        dut.io.issue.valid.poke(true.B)
        for (i <- 0 until dut.io.issue.bits.length) {
          dut.io.issue.bits(i).poke(data(i))
        }
        dut.clock.step(1)
        dut.io.issue.valid.poke(false.B)
      }

      def commit(expect: Seq[Valid[CommitBus]]): Unit = {
        dut.io.commit.ready.poke(true.B)
        for (i <- 0 until dut.io.commit.bits.length) {
          dut.io.commit.bits(i).expect(expect(i))
        }
        dut.clock.step(1)
        dut.io.commit.ready.poke(false.B)
      }

      def read(robTag: Int, expect: Int, valid: Boolean): Unit = {
        dut.io.read(0).robTag1.poke(intToUInt(robTag))
        dut.io.read(0).data1.bits.expect(intToUInt(expect))
        dut.io.read(0).data1.valid.expect(valid.B)
        dut.io.read(0).robTag2.poke(intToUInt(robTag))
        dut.io.read(0).data2.bits.expect(intToUInt(expect))
        dut.io.read(0).data2.valid.expect(valid.B)
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
      while (dut.io.issue.ready.peekBoolean()) {
        val data = Seq.fill(config.issueWidth)(reorderBufferEntry(Random.nextInt(), Random.nextInt(), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.bits.rd.getWidth).toInt + 1), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.bits.wb.getWidth).toInt + 1), Random.nextBoolean(), Random.nextBoolean()))
        issue(data)
        issues.enqueue(data)
      }

      dut.io.issue.ready.expect(false.B)
      
      // Read operands
      for (i <- 0 until config.robEntries) {
        val issue = issues(i / config.issueWidth)(i % config.issueWidth)
        val valid = issue.valid.litToBoolean.unary_!
        read(i, issue.bits.data.litValue.toInt, valid = valid)
        dut.clock.step(1)
      }
      
      // Write operands
      for (i <- 0 until config.robEntries) {
        val issue = issues(i / config.issueWidth)(i % config.issueWidth)
        write(i, writeData(i), writeTargets(i))
        read(i, writeData(i), true)
        dut.clock.step(1)
      }
      
      dut.io.commit.valid.expect(true.B)

      // Commit
      for (j <- 0 until issues.length) {
        val issue = issues.dequeue().toSeq
        val expected = chiselTypeOf(dut.io.commit.bits).zipWithIndex.map{
          case (cb, i) =>
            cb.Lit(
              _.bits.data -> intToUInt(writeData((config.issueWidth * j) + i)),
              _.bits.rd -> issue(i).bits.rd,
              _.bits.target -> intToUInt(writeTargets((config.issueWidth * j) + i)),
              _.bits.wb -> issue(i).bits.wb,
              _.bits.branched -> issue(i).bits.branched,
              _.valid -> issue(i).valid,
            )
        }
        commit(expected)
      }

      dut.io.commit.valid.expect(false.B)

      // Issue      
      while (dut.io.issue.ready.peekBoolean()) {
        val data = Seq.fill(config.issueWidth)(reorderBufferEntry(Random.nextInt(), Random.nextInt(), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.bits.rd.getWidth).toInt + 1), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.bits.wb.getWidth).toInt + 1), Random.nextBoolean(), Random.nextBoolean()))
        issue(data)
        issues.enqueue(data)
      }

      // Flush
      dut.io.flush.poke(true.B)
      dut.clock.step(1)
      dut.io.commit.valid.expect(false.B)
      dut.io.issue.ready.expect(true.B)
      
    }
  }
}
