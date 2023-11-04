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
  val dummyTag = 0

  val iterations = 1000
  var steps = 0
  val resetAfterPokes = true

  def step(clk: Clock, n: Int): Unit = {
    clk.step(n)
    steps += n
  }

  def issueBusFields(dut: ReservationStation, op: Int, tag1: Int, tag2: Int, data1: Int, data2: Int, imm: Int = 0): IssueBusFields = {
    chiselTypeOf(dut.rs.ib.head.data).Lit(
      _.op -> intToUInt(op),
      _.fields(0).tag -> intToUInt(tag1),
      _.fields(0).data -> intToUInt(data1),
      _.fields(1).tag -> intToUInt(tag2),
      _.fields(1).data -> intToUInt(data2),
      _.imm -> intToUInt(imm),
    )
  }

  def issue(dut: ReservationStation, issueData: IssueBusFields): Unit = {
    dut.rs.ib(0).tag.poke(dut.tag.U)
    dut.rs.ib(0).data.poke(issueData)
    step(dut.clock, 1)
    if (resetAfterPokes) {
      dut.rs.ib(0).tag.poke(dummyTag)
      dut.rs.ib(0).data.poke(issueBusFields(dut, 0, dummyTag, dummyTag, 0, 0))
    }
  }

  def issueData(dut: ReservationStation, op: Int, data1: Int, data2: Int, imm: Int = 0): Unit = {
    issue(dut, issueBusFields(dut, op, dut.tag, dut.tag, data1, data2, imm))
  }

  def writeCDB(dut: ReservationStation, tag: Int, data: Int): Unit = {
    dut.rs.cdb(0).valid.poke(true.B)
    dut.rs.cdb(0).bits.tag.poke(tag.U)
    dut.rs.cdb(0).bits.data.poke(intToUInt(data))
    step(dut.clock, 1)
    dut.rs.cdb(0).valid.poke(false.B)
    if (resetAfterPokes) {
      dut.rs.cdb(0).bits.tag.poke(dummyTag.U)
      dut.rs.cdb(0).bits.data.poke(0.U)
    }
  }

  def readCDB(dut: FunctionalUnit, expected: Int): Unit = {
    dut.fu.result.ready.poke(true.B)
    while (!dut.fu.result.valid.peekBoolean()) {
      step(dut.clock, 1)
    }
    dut.fu.result.bits.data.expect(intToUInt(expected))
    dut.fu.result.bits.tag.expect(dut.tag.U)
  }

  def issueExpect(dut: FunctionalUnit, op: Int, data1: Int, data2: Int, imm: Int = 0, expected: Int): Unit = {
    issueData(dut, op, data1, data2)
    readCDB(dut, expected)
    writeCDB(dut, dut.tag, expected)
  }
}