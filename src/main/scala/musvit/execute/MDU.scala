package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import musvit.common.OpCodes
import utility.Constants._
import utility.RisingEdge

class MDU(config: MusvitConfig) extends FunctionalUnit(config) {
}

class Multiplier(config: MusvitConfig, cycles: Int = 4) extends FunctionalUnit(config) {
  val resultReg = RegInit(0.U((2 * WORD_WIDTH).W))
  val countEn = WireDefault(false.B)
  val validReg = RegInit(false.B)
  val (counterValue, counterWrap) = Counter(0 until cycles, countEn, reset.asBool)

  when (RisingEdge(valid)) {
    countEn := true.B
  }.otherwise {
    countEn := counterValue =/= 0.U
    ready := false.B
  }

  val partialSums = Wire(Vec(WORD_WIDTH / cycles, UInt((WORD_WIDTH + (WORD_WIDTH / cycles)).W)))
  val data1Vec = VecInit(data1.asBools.grouped(WORD_WIDTH / cycles).map(VecInit(_).asUInt).toSeq)
  val data2Vec = VecInit(data2.asBools.grouped(WORD_WIDTH / cycles).map(VecInit(_).asUInt).toSeq)

  for (i <- 0 until WORD_WIDTH / cycles) {
    partialSums(i) := Mux(data2Vec(counterValue)(i).asBool, data1 << i, 0.U)
  }

  val shamtLookUp = VecInit(Seq.tabulate(cycles)( (i) => (i * (WORD_WIDTH / cycles)).U))
  val reducedSums = (partialSums.reduce((a: UInt, b: UInt) => (a + b)) << shamtLookUp(counterValue)).asTypeOf(UInt((2 * WORD_WIDTH).W))
  resultReg := Mux(counterValue === 0.U, reducedSums, resultReg + reducedSums)

  io.result.bits := Mux(op === MDU.MUL, resultReg.tail(WORD_WIDTH), resultReg.head(WORD_WIDTH))

  // Valid data logic
  when (counterWrap) {
    validReg := true.B
  }

  when (validReg && io.result.ready) {
    validReg := false.B
  }

  io.result.valid := validReg
}