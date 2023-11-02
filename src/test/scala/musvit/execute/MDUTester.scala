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
import musvit.common.OpCodes

class MultiplierTester extends AnyFlatSpec with ChiselScalatestTester with OpCodes {
  val config = MusvitConfig.default

  val moduleTag = 5.U
  val iterations = 1000
  var steps = 0

  "Multiplier" should "pass" in {
    test(new Multiplier(config))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        def step(n: Int): Unit = {
          dut.clock.step(n)
          steps += n
        }

        def fuData(op: UInt, data1: UInt, data2: UInt): FunctionalUnitOperands = {
          chiselTypeOf(dut.io.rs.bits).Lit(
            _.op -> op,
            _.data1 -> data1,
            _.data2 -> data2,
            )
        }

        def issue(operands: FunctionalUnitOperands): Unit = {
          dut.io.rs.bits.poke(operands)
          dut.io.rs.valid.poke(true.B)
          step(1)
          dut.io.rs.valid.poke(false.B)
        }

        def read(expected: UInt): Unit = {
          dut.io.result.ready.poke(true.B)
          while (!dut.io.result.valid.peekBoolean()) {
            step(1)
          }
          dut.io.result.bits.expect(expected)
        }

        def mul(data1: UInt, data2: UInt): Unit = {
          val data = fuData(MDU.MUL.value.U, data1, data2)
          issue(data)
          val product = data.data1.litValue * data.data2.litValue & 0x00000000ffffffffL
          read(product.U)
        }

        def mulh(data1: UInt, data2: UInt): Unit = {
          val data = fuData(MDU.MULH.value.U, data1, data2)
          issue(data)
          val product = data.data1.litValue * data.data2.litValue >> 32
          read(product.U)
        }

        def mulhsu(data1: UInt, data2: UInt): Unit = {
          val data = fuData(MDU.MULHSU.value.U, data1, data2)
          issue(data)
          val product = data.data1.litValue * data.data2.litValue.abs >> 32
          read(product.U)
        }

        def mulhu(data1: UInt, data2: UInt): Unit = {
          val data = fuData(MDU.MULHU.value.U, data1, data2)
          issue(data)
          val product = data.data1.litValue.abs * data.data2.litValue.abs >> 32
          read(product.U)
        }

        for (i <- 0 until iterations) {
          mul(getRandomWord(), getRandomWord())
          mulh(getRandomWord(), getRandomWord())
          mulhsu(getRandomWord(), getRandomWord())
          mulhu(getRandomWord(), getRandomWord())
        }

        println("Total steps was " + steps)
      }
  }
}
