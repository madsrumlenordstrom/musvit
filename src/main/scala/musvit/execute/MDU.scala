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
  val q = RegInit(0.U(WORD_WIDTH.W))
  val r = RegInit(0.U((WORD_WIDTH + 1).W))

  val countEn = WireDefault(false.B)
  val validReg = RegInit(false.B)
  val signReg = RegInit(false.B)
  val (counterValue, counterWrap) = Counter(WORD_WIDTH - 1 to 0 by -1, countEn, reset.asBool)
  val bitOH = UIntToOH(counterValue, WORD_WIDTH)

  when (RisingEdge(valid)) {
    countEn := true.B
    signReg := MuxCase(DontCare, Seq(
      (op === MDU.DIV)  -> (data1.head(1) ^ data2.head(1)),
      (op === MDU.DIVU) -> (false.B),
      (op === MDU.REM)  -> (data1.head(1) ^ data2.head(1)),
      (op === MDU.REMU) -> (false.B),
    ))
  }.otherwise {
    countEn := counterValue =/= (WORD_WIDTH - 1).U
    ready := false.B
  }

  val a = Mux(counterValue === (WORD_WIDTH - 1).U, data1, r)
  val b = (data2 << counterValue).asTypeOf(UInt((2 * WORD_WIDTH).W))
  val ge = a >= b
  r := Mux(ge, a - b, a)
  q := Mux(counterValue === (WORD_WIDTH - 1).U, (ge ## Fill(WORD_WIDTH - 1, "b0".U)), (Fill(WORD_WIDTH, ge.asUInt) & bitOH) | q)
  q := (q << 1) | ge.asUInt
  
  val unsignedResult = Mux(op === MDU.REM || op === MDU.REMU, r, q)
  
  io.result.bits := Mux(signReg, ~unsignedResult + 1.U, unsignedResult)

  // Valid data logic
  when (counterWrap) {
    validReg := true.B
  }
  
  when (validReg && io.result.ready) {
    validReg := false.B
  }

  io.result.valid := validReg
}