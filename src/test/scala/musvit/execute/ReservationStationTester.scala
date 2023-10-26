package musvit.execute

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import musvit.MusvitConfig
import utility.Functions._
import utility.Constants._
import os.group
import os.truncate

class ReservationStationTester extends AnyFlatSpec with ChiselScalatestTester {
  val config = MusvitConfig(fetchWidth = 2, rsNum = 16)
  val uniqID = 7.U

  val testFile = "random"
  val words = fileToUInts(testFile, WORD_WIDTH)

  "ReservationStation" should "pass" in {
    test(new ReservationStation(config, uniqID)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      def issue(qj: UInt, qk: UInt, vj: UInt, vk: UInt): Unit = {
        if (dut.io.fields.ready.peekBoolean()) {
          dut.io.fields.valid.poke(true.B)
          dut.io.fields.bits.busy.poke(true.B)
          dut.io.fields.bits.qj.poke(qj)
          dut.io.fields.bits.qk.poke(qk)
          dut.io.fields.bits.vj.poke(vj)
          dut.io.fields.bits.vk.poke(vk)
          dut.io.fields.bits.op.poke(2.U) // Random val
          dut.clock.step(1)
        }
      }

      def consumeOperands(expectOp1: UInt, expectOp2: UInt): Unit = {
        if (dut.io.operands.valid.peekBoolean()) {
          dut.io.operands.ready.poke(true.B)
          dut.io.operands.bits.op1.expect(expectOp1)
          dut.io.operands.bits.op2.expect(expectOp2)
        }
      }

      def setCDB(qi: Int, vi: UInt, valid: Bool): Unit = {
        dut.io.monitor(qi).valid.poke(valid)
        dut.io.monitor(qi).bits.vi.poke(vi)
      }

      def initCDB(vi: UInt, valid: Bool): Unit = {
        for (i <- 0 until dut.io.monitor.length) {
          setCDB(i, vi, valid)
        }
      }

      initCDB(0.U, false.B)

      // Check consume
      for (i <- 0 until words.length by 2) {
        issue(0.U, 0.U, words(i), words(i + 1))
        consumeOperands(words(i), words(i + 1))
      }

      issue(1.U, 2.U, 0.U, 0.U)
      dut.io.operands.valid.expect(false.B)
      dut.clock.step(5)
      setCDB(2, 0x0F0F0F0F.U, true.B)
      dut.clock.step(1)
      dut.io.operands.valid.expect(false.B)
      dut.io.operands.bits.op2.expect(0x0F0F0F0F.U)
      dut.clock.step(5)
      setCDB(1, 0x771100AA.U, true.B)
      dut.clock.step(1)
      dut.io.operands.valid.expect(true.B)
      dut.io.operands.bits.op1.expect(0x771100AA.U)
      consumeOperands(0x771100AA.U, 0x0F0F0F0F.U)
    }
  }
}