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

      // Test flush      
      while (dut.io.issue.ready.peekBoolean()) {
        val data = Seq.fill(config.fetchWidth)(reorderBufferEntry(true, Random.nextInt(), Random.nextInt(), Random.nextInt(bitWidthToUIntMax(dut.io.issue.bits.head.commit.inst.getWidth).toInt)))
        issue(data)
      }

    }
  }
}
