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
import musvit.common.ControlValues

class ALUTester extends FunctionalUnitTester {

  "ALU" should "pass" in {
    test(new ALU(config))
      .withAnnotations(annotations) { dut =>
        dut.clock.setTimeout(0)

        def add(data1: Int, data2: Int): Int = { data1 + data2 }

        def sub(data1: Int, data2: Int): Int = { data1 - data2 }

        def sll(data1: Int, data2: Int): Int = { data1 << data2 }

        def slt(data1: Int, data2: Int): Int = { if (data1 < data2) 1 else 0 }

        def sltu(data1: Int, data2: Int): Int = { if ((data1.toLong & 0xffffffffL) < (data2.toLong & 0xffffffffL)) 1 else 0 }

        def xor(data1: Int, data2: Int): Int = { data1 ^ data2 }

        def srl(data1: Int, data2: Int): Int = { data1 >>> data2 }

        def sra(data1: Int, data2: Int): Int = { data1 >> data2 }

        def or(data1: Int, data2: Int): Int = { data1 | data2 }

        def and(data1: Int, data2: Int): Int = { data1 & data2 }

        def beq(data1: Int, data2: Int): Int = { if (data1 == data2) 1 else 0 }

        def bne(data1: Int, data2: Int): Int = { if (data1 == data2) 0 else 1 }

        def blt(data1: Int, data2: Int): Int = { if (data1 < data2) 1 else 0 }

        def bge(data1: Int, data2: Int): Int = { if (data1 >= data2) 1 else 0 }

        def bltu(data1: Int, data2: Int): Int = { if ((data1.toLong & 0xffffffffL) < (data2.toLong & 0xffffffffL)) 1 else 0 }

        def bgeu(data1: Int, data2: Int): Int = { if ((data1.toLong & 0xffffffffL) >= (data2.toLong & 0xffffffffL)) 1 else 0 }

        def randomIssueExpectFromFunction(dut: FunctionalUnit, op: Int, func: (Int, Int) => Int): Unit = {
          issueExpectFromFunction(dut, op, Random.nextInt(), Random.nextInt(), func = func, target = 0)
        }

        def randomTest(): Unit = {
          println("Testing with random input")
          for (i <- 0 until iterations) {
            randomIssueExpectFromFunction(dut, ALU.ADD.value.toInt, func = add)
            randomIssueExpectFromFunction(dut, ALU.SUB.value.toInt, func = sub)
            randomIssueExpectFromFunction(dut, ALU.SLL.value.toInt, func = sll)
            randomIssueExpectFromFunction(dut, ALU.SLT.value.toInt, func = slt)
            randomIssueExpectFromFunction(dut, ALU.SLTU.value.toInt, func = sltu)
            randomIssueExpectFromFunction(dut, ALU.XOR.value.toInt, func = xor)
            randomIssueExpectFromFunction(dut, ALU.SRL.value.toInt, func = srl)
            randomIssueExpectFromFunction(dut, ALU.SRA.value.toInt, func = sra)
            randomIssueExpectFromFunction(dut, ALU.OR.value.toInt,  func = or)
            randomIssueExpectFromFunction(dut, ALU.AND.value.toInt, func = and)

            randomIssueExpectFromFunction(dut, ALU.BEQ.value.toInt, func = beq)
            randomIssueExpectFromFunction(dut, ALU.BNE.value.toInt, func = bne)
            randomIssueExpectFromFunction(dut, ALU.BLT.value.toInt, func = blt)
            randomIssueExpectFromFunction(dut, ALU.BGE.value.toInt, func = bge)
            randomIssueExpectFromFunction(dut, ALU.BLTU.value.toInt, func = bltu)
            randomIssueExpectFromFunction(dut, ALU.BGEU.value.toInt, func = bgeu)
          }
        }

        def edgeCases(): Unit = {
          println("Testing edge cases")
          val edgeVals = Seq(1, -1)
          for (i <- 0 until 2) {
            for (j <- 0 until 2) {
              issueExpectFromFunction(dut, ALU.ADD.value.toInt, edgeVals(i), edgeVals(j), func = add)
              issueExpectFromFunction(dut, ALU.SUB.value.toInt, edgeVals(i), edgeVals(j), func = sub)
              issueExpectFromFunction(dut, ALU.SLL.value.toInt, edgeVals(i), edgeVals(j), func = sll)
              issueExpectFromFunction(dut, ALU.SLT.value.toInt, edgeVals(i), edgeVals(j), func = slt)
              issueExpectFromFunction(dut, ALU.SLTU.value.toInt, edgeVals(i), edgeVals(j), func = sltu)
              issueExpectFromFunction(dut, ALU.XOR.value.toInt, edgeVals(i), edgeVals(j), func = xor)
              issueExpectFromFunction(dut, ALU.SRL.value.toInt, edgeVals(i), edgeVals(j), func = srl)
              issueExpectFromFunction(dut, ALU.SRA.value.toInt, edgeVals(i), edgeVals(j), func = sra)
              issueExpectFromFunction(dut, ALU.OR.value.toInt, edgeVals(i), edgeVals(j), func = or)
              issueExpectFromFunction(dut, ALU.AND.value.toInt, edgeVals(i), edgeVals(j), func = and)

              issueExpectFromFunction(dut, ALU.BEQ.value.toInt, edgeVals(i), edgeVals(j), func = beq)
              issueExpectFromFunction(dut, ALU.BNE.value.toInt, edgeVals(i), edgeVals(j), func = bne)
              issueExpectFromFunction(dut, ALU.BLT.value.toInt, edgeVals(i), edgeVals(j), func = blt)
              issueExpectFromFunction(dut, ALU.BGE.value.toInt, edgeVals(i), edgeVals(j), func = bge)
              issueExpectFromFunction(dut, ALU.BLTU.value.toInt, edgeVals(i), edgeVals(j), func = bltu)
              issueExpectFromFunction(dut, ALU.BGEU.value.toInt, edgeVals(i), edgeVals(j), func = bgeu)
            }
          }
        }

        randomTest()
        edgeCases()

        println("Total steps was " + steps)
      }
  }
}
