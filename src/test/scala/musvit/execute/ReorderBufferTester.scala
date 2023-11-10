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

class ReorderBufferTester extends AnyFlatSpec with ChiselScalatestTester {
  val config = MusvitConfig(
    fetchWidth = 2,
    robEntries = 32,
  )

  "ReorderBuffer" should "pass" in {
    test(new ReorderBuffer(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)
      dut.io.flush.poke(false.B)

      val issues = Queue[Seq[ReorderBufferEntry]]()

      def reorderBufferEntry(ready: Boolean, data: Int, addr: Int, inst: Int): ReorderBufferEntry = {
        chiselTypeOf(dut.io.issue.bits.head).Lit(
          _.ready -> ready.B,
          _.commit.data -> intToUInt(data),
          _.commit.addr -> intToUInt(addr),
          _.commit.inst -> intToUInt(inst),
        )
      }

      def reorderBufferEntryToCommitBus(data: ReorderBufferEntry): CommitBus = {
        chiselTypeOf(dut.io.commit.bits.head).Lit(
          _ -> data.commit
        )
      }

      def issue(data: Seq[ReorderBufferEntry]): Unit = {
        dut.io.issue.valid.poke(true.B)
        for (i <- 0 until dut.io.issue.bits.length) {
          dut.io.issue.bits(i).poke(data(i))
        }
        dut.clock.step(1)
        dut.io.issue.valid.poke(false.B)
      }

      def commit(expect: Seq[CommitBus]): Unit = {
        dut.io.commit.ready.poke(true.B)
        for (i <- 0 until dut.io.commit.bits.length) {
          dut.io.commit.bits(i).expect(expect(i))
        }
        dut.clock.step(1)
        dut.io.commit.ready.poke(false.B)
      }

      def read(robTag: Int, expect: Int): Unit = {
        dut.io.read(0).robTag1.poke(intToUInt(robTag))
        dut.io.read(0).data1.bits.expect(intToUInt(expect))
        dut.io.read(0).data1.valid.expect(true.B)
        dut.io.read(0).robTag2.poke(intToUInt(robTag))
        dut.io.read(0).data2.bits.expect(intToUInt(expect))
        dut.io.read(0).data2.valid.expect(true.B)
      }

      def write(robTag: Int, data: Int): Unit = {
        dut.io.cdb(0).valid.poke(true.B)
        dut.io.cdb(0).bits.tag.poke(intToUInt(robTag))
        dut.io.cdb(0).bits.data.poke(intToUInt(data))
        dut.clock.step(1)
      }

      // Issue      
      while (dut.io.issue.ready.peekBoolean()) {
        val data = Seq.fill(config.fetchWidth)(reorderBufferEntry(true, Random.nextInt(), Random.nextInt(), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.commit.inst.getWidth).toInt)))
        issue(data)
        issues.enqueue(data)
      }

      // Commit
      while (dut.io.commit.valid.peekBoolean()) {
        val expected = issues.dequeue().map(_.commit).toSeq
        commit(expected)
      }

      // Issue      
      while (dut.io.issue.ready.peekBoolean()) {
        val data = Seq.fill(config.fetchWidth)(reorderBufferEntry(true, Random.nextInt(), Random.nextInt(), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.commit.inst.getWidth).toInt)))
        issue(data)
        issues.enqueue(data)
      }

      // Read operands
      for (i <- 0 until config.robEntries) {
        read(i, issues(i / config.fetchWidth)(i % config.fetchWidth).commit.data.litValue.toInt)
        dut.clock.step(1)
      }

      // Write operands
      for (i <- 0 until config.robEntries) {
        val writeVal = Random.nextInt()
        write(i, writeVal)
        read(i, writeVal)
        dut.clock.step(1)
      }

      // Flush
      dut.io.commit.valid.expect(true.B)
      dut.io.issue.ready.expect(false.B)
      dut.io.flush.poke(true.B)
      dut.clock.step(1)
      dut.io.commit.valid.expect(false.B)
      dut.io.issue.ready.expect(true.B)
      
    }
  }
}
