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

class ALUTester extends AnyFlatSpec with ChiselScalatestTester with OpCodes {
  val config = MusvitConfig.default

  val iterations = 1000
  var steps = 0

  "ALU" should "pass" in {
    test(new ALU(config))
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
        }

        def read(expected: UInt): Unit = {
          dut.io.result.ready.poke(true.B)
          while (!dut.io.result.valid.peekBoolean()) {
            step(1)
          }
          dut.io.result.bits.expect(expected)
        }

        def add(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.ADD.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 + data2
          read(intToUInt(product))
        }

        def sub(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.SUB.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 - data2
          read(intToUInt(product))
        }

        def sll(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.SLL.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 << data2
          read(intToUInt(product))
        }

        def slt(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.SLT.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = if (data1 < data2) 1 else 0
          read(intToUInt(product))
        }

        def sltu(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.SLTU.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = if ((data1.toLong & 0xffffffffL) < (data2.toLong & 0xffffffffL)) 1 else 0
          read(intToUInt(product))
        }

        def xor(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.XOR.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 ^ data2
          read(intToUInt(product))
        }

        def srl(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.SRL.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 >>> data2
          read(intToUInt(product))
        }

        def sra(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.SRA.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 >> data2
          read(intToUInt(product))
        }

        def or(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.OR.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 | data2
          read(intToUInt(product))
        }

        def and(data1: Int, data2: Int): Unit = {
          val data = fuData(ALU.AND.value.U, intToUInt(data1), intToUInt(data2))
          issue(data)
          val product = data1 & data2
          read(intToUInt(product))
        }

        def randomTest(): Unit = {
          println("Testing with random input")
          for (i <- 0 until iterations) {
            add(Random.nextInt(), Random.nextInt())
            sub(Random.nextInt(), Random.nextInt())
            sll(Random.nextInt(), Random.nextInt())
            slt(Random.nextInt(), Random.nextInt())
            sltu(Random.nextInt(), Random.nextInt())
            xor(Random.nextInt(), Random.nextInt())
            srl(Random.nextInt(), Random.nextInt())
            sra(Random.nextInt(), Random.nextInt())
            or(Random.nextInt(), Random.nextInt())
            and(Random.nextInt(), Random.nextInt())
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              add(edgeVals(i), edgeVals(j))
              sub(edgeVals(i), edgeVals(j))
              sll(edgeVals(i), edgeVals(j))
              slt(edgeVals(i), edgeVals(j))
              sltu(edgeVals(i), edgeVals(j))
              xor(edgeVals(i), edgeVals(j))
              srl(edgeVals(i), edgeVals(j))
              sra(edgeVals(i), edgeVals(j))
              or(edgeVals(i), edgeVals(j))
              and(edgeVals(i), edgeVals(j))
            }
          }
        }

        randomTest()
        edgeCases()

        println("Total steps was " + steps)
      }
  }
}