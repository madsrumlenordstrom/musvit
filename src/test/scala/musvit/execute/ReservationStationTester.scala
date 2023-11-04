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

class ReservationStationTester extends FunctionalUnitTester {
  "ReservationStation" should "pass" in {
    test(new TestingReservationStation(config, defaultTag)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      def getRandomIBData(): IssueBusFields = {
        issueBusFields(dut,
        Random.nextInt(bitWidthToUIntMax(dut.rs.ib(0).data.op.getWidth).toInt),
        Random.nextInt(bitWidthToUIntMax(dut.rs.ib(0).tag.getWidth).toInt),
        Random.nextInt(bitWidthToUIntMax(dut.rs.ib(0).tag.getWidth).toInt),
        Random.nextInt(),
        Random.nextInt(),
        )
      }

      def fuData(op: Int, data1: Int, data2: Int): FunctionalUnitOperands = {
        chiselTypeOf(dut.debug.bits).Lit(
          _.op -> intToUInt(op),
          _.data1 -> intToUInt(data1),
          _.data2 -> intToUInt(data2),
          )
      }

      def readFU(expected: FunctionalUnitOperands): Unit = {
        dut.debug.ready.poke(true.B)
        dut.debug.valid.expect(true.B)
        dut.debug.bits.expect(expected)
        step(dut.clock, 1)
        dut.debug.ready.poke(false.B)
      }

      for (i <- 0 until iterations) {
        val data = getRandomIBData()
        var expected1 = data.fields(0).data.litValue.toInt
        var expected2 = data.fields(1).data.litValue.toInt
        issue(dut, data)

        var j = 0
        while (!dut.debug.valid.peekBoolean()) {
          if (j == defaultTag) {
            j += 1
          }
          val newData = Random.nextInt()
          writeCDB(dut, j, newData)
          if (j == data.fields(0).tag.litValue && data.fields(0).data.litValue.toInt != defaultTag) {
            expected1 = newData
          }
          if (j == data.fields(1).tag.litValue && data.fields(1).data.litValue.toInt != defaultTag) {
            expected2 = newData
          }
          j += 1
        }
        // Check output
        readFU(fuData(data.op.litValue.toInt, expected1, expected2))

      }
      println("Total steps was " + steps)
    }
  }
}