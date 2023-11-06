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
import musvit.common.ControlSignals

class FunctionalUnitTester extends AnyFlatSpec with ChiselScalatestTester with ControlSignals {
  val config = MusvitConfig.default
  val dummyTag = 0

  val iterations = 100
  var steps = 0
  val resetAfterPokes = true
  val annotations = Seq(WriteVcdAnnotation)

  def step(clk: Clock, n: Int): Unit = {
    clk.step(n)
    steps += n
  }

  def issueSource(dut: ReservationStation, data: Int, valid: Boolean, tag: Int): IssueSource = {
    chiselTypeOf(dut.rs.ib.head.src1).Lit(
      _.data -> intToUInt(data),
      _.valid -> valid.B,
      _.tag -> intToUInt(tag),
    )
  }

  def getRandomIssueSource(dut: ReservationStation): IssueSource = {
    issueSource(dut,
      data = Random.nextInt(),
      valid = Random.nextBoolean(),
      tag = Random.nextInt(bitWidthToUIntMax(dut.rs.ib.head.robTag.getWidth).toInt)
    )
  }

  def issueBus(dut: ReservationStation, op: Int, src1: IssueSource, src2: IssueSource, robTag: Int, imm: Int = 0): IssueBus = {
    chiselTypeOf(dut.rs.ib.head).Lit(
      _.op -> intToUInt(op),
      _.src1 -> src1,
      _.src2 -> src2,
      _.robTag -> intToUInt(robTag),
      _.imm -> intToUInt(imm),
    )
  }

  def getRandomIssueBus(dut: ReservationStation): IssueBus = {
    issueBus(dut,
      op = Random.nextInt(bitWidthToUIntMax(dut.rs.ib.head.op.getWidth).toInt),
      src1 = getRandomIssueSource(dut),
      src2 = getRandomIssueSource(dut),
      robTag = Random.nextInt(bitWidthToUIntMax(dut.rs.ib.head.robTag.getWidth).toInt),
      imm = Random.nextInt(),
    )
  }

  def issue(dut: ReservationStation, issueData: IssueBus, ibIdx: Int = 0): Unit = {
    dut.rs.ib(ibIdx).poke(issueData)
    dut.rs.ibIdx.poke(ibIdx)
    dut.rs.writeEn.poke(true.B)
    step(dut.clock, 1)
    dut.rs.writeEn.poke(false.B)
    if (resetAfterPokes) {
      val dummyData = issueSource(dut, 0, false, dummyTag)
      dut.rs.ib(ibIdx).poke(issueBus(dut, 0, dummyData, dummyData, 0, 0))
    }
  }

  def issueData(dut: ReservationStation, op: Int, data1: Int, data2: Int, robTag: Int = dummyTag, imm: Int = 0): Unit = {
    val src1 = issueSource(dut, data1, true, dummyTag)
    val src2 = issueSource(dut, data2, true, dummyTag)
    issue(dut, issueBus(dut, op, src1, src2, robTag, imm), Random.nextInt(config.fetchWidth))
  }

  def writeCDB(dut: ReservationStation, tag: Int, data: Int, cdbIdx: Int = 0): Unit = {
    dut.rs.cdb(cdbIdx).valid.poke(true.B)
    dut.rs.cdb(cdbIdx).bits.tag.poke(tag.U)
    dut.rs.cdb(cdbIdx).bits.data.poke(intToUInt(data))
    step(dut.clock, 1)
    dut.rs.cdb(cdbIdx).valid.poke(false.B)
    if (resetAfterPokes) {
      dut.rs.cdb(cdbIdx).bits.tag.poke(dummyTag.U)
      dut.rs.cdb(cdbIdx).bits.data.poke(0.U)
    }
  }

  def readCDB(dut: FunctionalUnit, expected: Int, robTag: Int): Unit = {
    dut.fu.result.ready.poke(true.B)
    while (!dut.fu.result.valid.peekBoolean()) {
      step(dut.clock, 1)
    }
    dut.fu.result.bits.data.expect(intToUInt(expected))
    dut.fu.result.bits.tag.expect(intToUInt(robTag))
  }

  def issueExpect(dut: FunctionalUnit, op: Int, data1: Int, data2: Int, robTag: Int = Random.nextInt(config.robEntries), imm: Int = 0, expected: Int): Unit = {
    issueData(dut, op, data1, data2, robTag, imm)
    readCDB(dut, expected, robTag)
  }

  def issueExpectFromFunction(dut: FunctionalUnit, op: Int, data1: Int, data2: Int, robTag: Int = Random.nextInt(config.robEntries), imm: Int = 0, func: (Int, Int) => Int): Unit = {
    issueExpect(dut, op, data1, data2, robTag, imm, func(data1, data2))
  }
}
