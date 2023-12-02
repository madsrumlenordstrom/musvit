package io

import chisel3._
import chisel3.util.{is, log2Up, switch, Counter, Decoupled, MuxLookup, UIntToOH}

object BinaryToBCD {
  object State extends ChiselEnum {
    val idle, running = Value
  }
}

class BinaryToBCD(width: Int) extends Module {
  import BinaryToBCD.State
  import BinaryToBCD.State._

  def calcNumOfNibbles(width: Int): Int = {
    val maxValue = (1L << width.toLong) - 1L
    var numOfNibbles = 1
    var tmpValue = 10L
    while (tmpValue < maxValue) {
      tmpValue *= 10L
      numOfNibbles += 1
    }
    numOfNibbles
  }

  val numOfNibbles = calcNumOfNibbles(width)
  val numOfCycles = width

  val io = IO(new Bundle {
    val binary = Flipped(Decoupled(UInt(width.W)))
    val bcd = Output(UInt((4 * numOfNibbles).W))
  })

  val scratchPad = RegInit(0.U(((numOfNibbles * 4) + width).W))
  val shiftsReg = RegInit(0.U(log2Up(width).W))
  val bcdReg = RegInit(0.U((4 * numOfNibbles).W))
  val state = RegInit(idle)
  val addsVec = Wire(Vec(numOfNibbles, UInt(4.W)))

  for (i <- 0 until numOfNibbles) {
    addsVec(i) := Mux(scratchPad(i * 4 + width + 3, i * 4 + width) >= 5.U(4.W), 3.U(4.W), 0.U(4.W))
  }

  io.bcd := scratchPad((numOfNibbles * 4) + width - 1, width)
  io.binary.ready := WireDefault(true.B)

  switch(state) {
    is(idle) {
      io.binary.ready := true.B
      when(io.binary.valid) {
        state := running
        scratchPad := 0.U(numOfNibbles.W) ## io.binary.bits
        shiftsReg := 0.U
        bcdReg := scratchPad((numOfNibbles * 4) + width - 1, width)
      }
    }
    is(running) {
      io.binary.ready := false.B
      io.bcd := bcdReg
      scratchPad := ((addsVec.asUInt << width) + scratchPad) << 1
      shiftsReg := shiftsReg + 1.U
      when(shiftsReg === (numOfCycles - 1).U) {
        state := idle
      }
    }
  }
}

class SevenSegmentDecoder extends Module {
  val io = IO(new Bundle {
    val value = Input(UInt(4.W))
    val segment = Output(UInt(7.W))
  })

  io.segment := MuxLookup(io.value, 0.U)(
    Seq(
      0.U -> "b0111111".U,
      1.U -> "b0000110".U,
      2.U -> "b1011011".U,
      3.U -> "b1001111".U,
      4.U -> "b1100110".U,
      5.U -> "b1101101".U,
      6.U -> "b1111101".U,
      7.U -> "b0000111".U,
      8.U -> "b1111111".U,
      9.U -> "b1101111".U,
      10.U -> "b1011111".U,
      11.U -> "b1111100".U,
      12.U -> "b0111001".U,
      13.U -> "b1011110".U,
      14.U -> "b1111001".U,
      15.U -> "b1110001".U
    )
  )
}

class MultiplexedSevenSegmentDriverIO(numOfDigits: Int) extends Bundle {
  val value = Input(UInt((numOfDigits * 4).W))
  val segment = Output(UInt(7.W))
  val segSel = Output(UInt(numOfDigits.W))
}

class MultiplexedSevenSegmentDriver(
  numOfDigits: Int,
  clkFreq:     Int,
  invSegSel:   Boolean = false,
  invSegment:  Boolean = false,
  binaryToBCD: Boolean = false)
    extends Module {
  val io = IO(new MultiplexedSevenSegmentDriverIO(numOfDigits))

  val CNT_MAX = ((clkFreq / 1000) - 1)
  val (countValue, countTick) = Counter(0 to CNT_MAX)
  val segmentReg = RegInit(0.U(log2Up(numOfDigits).W))

  when(countTick === true.B && segmentReg === (numOfDigits - 1).U) {
    segmentReg := 0.U
  }.elsewhen(countTick === true.B) {
    segmentReg := segmentReg + 1.U
  }

  val sevSegDecoder = Module(new SevenSegmentDecoder)
  if (binaryToBCD) {
    val binaryToBCD = Module(new BinaryToBCD(numOfDigits * 4))
    binaryToBCD.io.binary.valid := (countValue === (CNT_MAX - binaryToBCD.numOfCycles).U)
    binaryToBCD.io.binary.bits := io.value
    sevSegDecoder.io.value := (binaryToBCD.io.bcd >> (segmentReg << 2))(3, 0)
  } else {
    sevSegDecoder.io.value := (io.value >> (segmentReg << 2))(3, 0)
  }

  if (invSegSel) {
    io.segSel := ~UIntToOH(segmentReg)
  } else {
    io.segSel := UIntToOH(segmentReg)
  }

  if (invSegment) {
    io.segment := ~sevSegDecoder.io.segment
  } else {
    io.segment := sevSegDecoder.io.segment
  }
}
