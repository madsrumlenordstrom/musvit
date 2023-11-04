package musvit.execute

import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

import musvit.MusvitConfig
import utility.Functions._
import utility.Constants._
import utility.TestingFunctions._

class ReservationStationTester extends AnyFlatSpec with ChiselScalatestTester {
  val config = MusvitConfig.default

  val moduleTag = 5
  val iterations = 1000

  "ReservationStation" should "pass" in {
    test(new TestingReservationStation(config, moduleTag)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      def rsData(op: UInt, tag1: UInt, tag2: UInt, data1: UInt, data2: UInt): IssueBusFields = {
        chiselTypeOf(dut.rs.ib.head.data).Lit(_.op -> op,
        _.fields(0).tag -> tag1,
        _.fields(0).data -> data1,
        _.fields(1).tag -> tag2,
        _.fields(1).data -> data2
        )
      }

      def getRandomRsData(): IssueBusFields = {
        rsData(getRandomData(dut.rs.ib(0).data.op.getWidth),
        getRandomData(dut.rs.ib(0).tag.getWidth),
        getRandomData(dut.rs.ib(0).tag.getWidth),
        getRandomWord(),
        getRandomWord())
      }

      def fuData(op: UInt, data1: UInt, data2: UInt): FunctionalUnitOperands = {
        chiselTypeOf(dut.debug.bits).Lit(
          _.op -> op,
          _.data1 -> data1,
          _.data2 -> data2,
          )
      }

      def issue(tag: UInt, issueData: IssueBusFields): Unit = {
        dut.rs.ib(0).tag.poke(tag)
        dut.rs.ib(0).data.poke(issueData)
        dut.clock.step(1)
      }

      def read(expected: FunctionalUnitOperands): Unit = {
        dut.debug.ready.poke(true.B)
        dut.debug.valid.expect(true.B)
        dut.debug.bits.expect(expected)
      }

      def writeCDB(tag: UInt, data: UInt): Unit = {
        dut.rs.cdb(0).valid.poke(true.B)
        dut.rs.cdb(0).bits.tag.poke(tag)
        dut.rs.cdb(0).bits.data.poke(data)
        dut.clock.step(1)
      }

      for (i <- 0 until iterations) {
        val data = getRandomRsData()
        var expected1 = data.fields(0).data
        var expected2 = data.fields(1).data
        issue(moduleTag.U, data)

        var j = 0
        while (!dut.debug.valid.peekBoolean()) {
          if (j == moduleTag) {
            j += 1
          }
          val newData = getRandomWord()
          writeCDB(j.U, newData)
          if (j == data.fields(0).tag.litValue && data.fields(0).data.litValue != moduleTag) {
            expected1 = newData
          }
          if (j == data.fields(1).tag.litValue && data.fields(1).data.litValue != moduleTag) {
            expected2 = newData
          }
          j += 1
        }
        // Check output
        read(fuData(data.op, expected1, expected2))

        // Reset reservation station
        writeCDB(moduleTag.U, 0x00.U)
      }
    }
  }
}