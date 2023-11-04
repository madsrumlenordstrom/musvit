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

class FunctionalUnitTester extends AnyFlatSpec with ChiselScalatestTester with OpCodes {
  val config = MusvitConfig.default
  val defaultTag = 5

  val iterations = 1000
  var steps = 0

  def step(clk: Clock, n: Int): Unit = {
    clk.step(n)
    steps += n
  }

  def issueBusFields(dut: FunctionalUnit, op: Int, tag1: Int, tag2: Int, data1: Int, data2: Int): IssueBusFields = {
    chiselTypeOf(dut.rs.ib.head.data).Lit(
      _.op -> intToUInt(op),
      _.fields(0).tag -> intToUInt(tag1),
      _.fields(0).data -> intToUInt(data1),
      _.fields(1).tag -> intToUInt(tag2),
      _.fields(1).data -> intToUInt(data2),
    )
  }

  def issue(dut: FunctionalUnit, issueData: IssueBusFields): Unit = {
    dut.rs.ib(0).tag.poke(dut.tag.U)
    dut.rs.ib(0).data.poke(issueData)
    step(dut.clock, 1)
  }

  def issueData(dut: FunctionalUnit, op: Int, data1: Int, data2: Int): Unit = {
    issue(dut, issueBusFields(dut, op, dut.tag, dut.tag, data1, data2))
  }

  def writeCDB(dut: FunctionalUnit, tag: Int, data: Int): Unit = {
    dut.rs.cdb(0).valid.poke(true.B)
    dut.rs.cdb(0).bits.tag.poke(tag.U)
    dut.rs.cdb(0).bits.data.poke(intToUInt(data))
    dut.clock.step(1)
  }

  def readCDB(dut: FunctionalUnit, expected: Int): Unit = {
    dut.fu.result.ready.poke(true.B)
    while (!dut.fu.result.valid.peekBoolean()) {
      step(dut.clock, 1)
    }
    dut.fu.result.bits.data.expect(intToUInt(expected))
    dut.fu.result.bits.tag.expect(dut.tag.U)
  }

  def issueExpect(dut: FunctionalUnit, op: Int, data1: Int, data2: Int, expected: Int): Unit = {
    issueData(dut, op, data1, data2)
    readCDB(dut, expected)
    writeCDB(dut, dut.tag, expected)
  }
}