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

class ALUTester extends FunctionalUnitTester {

  "ALU" should "pass" in {
    test(new ALU(config, defaultTag))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.setTimeout(0)

        def add(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.ADD.value.toInt, data1, data2, data1 + data2)
        }

        def sub(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.SUB.value.toInt, data1, data2, data1 - data2)
        }

        def sll(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.SLL.value.toInt, data1, data2, data1 << data2)
        }

        def slt(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.SLT.value.toInt, data1, data2, if (data1 < data2) 1 else 0)
        }

        def sltu(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.SLTU.value.toInt, data1, data2, if ((data1.toLong & 0xffffffffL) < (data2.toLong & 0xffffffffL)) 1 else 0)
        }

        def xor(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.XOR.value.toInt, data1, data2, data1 ^ data2)
        }

        def srl(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.SRL.value.toInt, data1, data2, data1 >>> data2)
        }

        def sra(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.SRA.value.toInt, data1, data2, data1 >> data2)
        }

        def or(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.OR.value.toInt, data1, data2, data1 | data2)
        }

        def and(data1: Int, data2: Int): Unit = {
           issueExpect(dut, ALU.AND.value.toInt, data1, data2, data1 & data2)
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
