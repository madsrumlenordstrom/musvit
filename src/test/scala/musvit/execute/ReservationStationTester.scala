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
    test(new TestingReservationStation(config)).withAnnotations(annotations) { dut =>
      dut.clock.setTimeout(0)

      def readRS(expected: IssueBus): Unit = {
        dut.debug.ready.poke(true.B)
        dut.debug.valid.expect(true.B)
        dut.debug.bits.expect(expected)
        step(dut.clock, 1)
        dut.debug.ready.poke(false.B)
      }

      for (i <- 0 until iterations) {
        val data = getRandomIssueBus(dut)
        var expected1 = data.src1.data.litValue.toInt
        var expected2 = data.src2.data.litValue.toInt
        issue(dut, data)

        var j = 0
        while (!dut.debug.valid.peekBoolean()) {
          val newData = Random.nextInt()
          if (j == data.src1.robTag.litValue && !dut.debug.bits.src1.data.valid.peekBoolean()) {
            expected1 = newData
          }
          if (j == data.src2.robTag.litValue && !dut.debug.bits.src2.data.valid.peekBoolean()) {
            expected2 = newData
          }
          writeCDB(dut, j, newData, Random.nextInt(config.fetchWidth))
          j += 1
        }
        // Check output
        val src1 = issueSource(dut, expected1, true, data.src1.robTag.litValue.toInt)
        val src2 = issueSource(dut, expected2, true, data.src2.robTag.litValue.toInt)
        readRS(issueBus(dut, data.op.litValue.toInt, src1, src2, data.robTag.litValue.toInt, data.imm.litValue.toInt))

      }
      println("Total steps was " + steps)
    }
  }
}
