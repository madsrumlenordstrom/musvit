package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import musvit.common.OpCodes
import utility.Constants._
import utility.RisingEdge
import utility.Negate
import utility.SignExtend

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

class Divider(config: MusvitConfig) extends FunctionalUnit(config) {
  val quotientReg = RegInit(0.U(WORD_WIDTH.W))
  val remainderReg = RegInit(0.U((WORD_WIDTH + 1).W))
  val divisorReg = RegInit(0.U((2 * WORD_WIDTH).W))
  val signReg = RegInit(false.B)
  val validReg = RegInit(false.B)

  val countEn = WireDefault(false.B)
  val (counterValue, counterWrap) = Counter(0 until WORD_WIDTH, countEn, reset.asBool)
  val counterIsInit = counterValue === 0.U

  when (RisingEdge(valid)) {
    countEn := true.B
    signReg := Mux(op === MDU.DIV || op === MDU.REM, data1.head(1) ^ data2.head(1), false.B)
  }.otherwise {
    countEn := !counterIsInit
    ready := false.B
  }

  val dividend = Mux(counterIsInit, data1, remainderReg)
  val divisor = Mux(counterIsInit, data2 ## Fill(WORD_WIDTH - 1, 0.U), divisorReg)
  val ge = dividend >= divisor
  remainderReg := Mux(ge, dividend - divisor, dividend)
  quotientReg := (quotientReg << 1) | ge.asUInt
  divisorReg := divisor >> 1
  
  val unsignedResult = Mux(op === MDU.REM || op === MDU.REMU, remainderReg, quotientReg)
  
  io.result.bits := Mux(signReg, Negate(unsignedResult), unsignedResult)

  // Valid data logic
  when (counterWrap) {
    validReg := true.B
  }
  
  when (validReg && io.result.ready) {
    validReg := false.B
  }

  io.result.valid := validReg
}

class Divider2(config: MusvitConfig) extends FunctionalUnit(config) {
  //val resultReg = RegInit(0.U((2 * WORD_WIDTH + 1).W))
  val remainderReg = RegInit(0.U((WORD_WIDTH + 1).W))
  val quotientReg = RegInit(0.U(WORD_WIDTH.W))
  val divisorReg = RegInit(0.U(WORD_WIDTH.W))
  

  val countEn = WireDefault(false.B)
  val validReg = RegInit(false.B)
  val signReg = RegInit(false.B)
  val (counterValue, counterWrap) = Counter(0 until WORD_WIDTH + 1, countEn, reset.asBool)
  val counterIsInit = counterValue === 0.U

  when (RisingEdge(valid)) {
    countEn := true.B
    signReg := MuxCase(DontCare, Seq(
      (op === MDU.DIV)  -> (data1.head(1) ^ data2.head(1)),
      (op === MDU.DIVU) -> (false.B),
      (op === MDU.REM)  -> (data1.head(1) ^ data2.head(1)),
      (op === MDU.REMU) -> (false.B),
    ))
  }.otherwise {
    countEn := !counterIsInit
    ready := false.B
  }

  val divisor = Mux(counterIsInit, data2, divisorReg)
  divisorReg := divisor >> 1
  val remainder = Mux(counterIsInit, data1.head(1) ## data1, remainderReg)
  val divRemSum = remainderReg -& divisorReg
  val ltz = divRemSum.head(1).asBool
  remainderReg := Mux(ltz, remainderReg, divRemSum)
  quotientReg := (quotientReg << 1) | ltz.unary_!.asUInt


  val unsignedResult = Mux(op === MDU.REM || op === MDU.REMU, remainderReg.tail(WORD_WIDTH), quotientReg)

  io.result.bits := Mux(signReg, Negate(unsignedResult), unsignedResult)

  // Valid data logic
  when (counterWrap) {
    validReg := true.B
  }

  when (validReg && io.result.ready) {
    validReg := false.B
  }

  io.result.valid := validReg
}