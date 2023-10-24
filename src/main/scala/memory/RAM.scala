package memory

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import musvit.MusvitConfig
import musvit.common.OpCodes
import utility.Constants._
import utility.{BitsToByteVec, BarrelShifter, SignExtend}

class RAMIO(size: Int, width: Int) extends Bundle {
  val readAddr      = Input(UInt(log2Up(size).W))
  val readData      = Output(UInt(width.W))
  val readEn        = Input(Bool())
  val writeAddr     = Input(UInt(log2Up(size).W))
  val writeData     = Input(UInt(width.W))
  val writeEn       = Input(Bool())
}

class RAM(size: Int, width: Int) extends Module {
  val io = IO(new RAMIO(size, width))

  val mem  = SyncReadMem(size, UInt(width.W))

  when (io.writeEn) {
    mem.write(io.writeAddr, io.writeData)
  }

  // Bypassing
  val bypass = RegNext(io.writeEn && io.readEn && (io.writeAddr === io.readData))
  val writeDataReg = RegNext(io.writeData)


  io.readData := Mux(bypass, writeDataReg, mem.read(io.readAddr, io.readEn))
}

object RAM {
  def apply(size: Int, width: Int) = {
    Module(new RAM(size, width))
  }
}

class MusvitRAMIO(config: MusvitConfig) extends Bundle with OpCodes {
  val addr          = Input(UInt(log2Up(config.ramSize).W))
  val en            = Input(Bool())
  val isWrite       = Input(Bool())
  val isSigned      = Input(Bool())
  val dataWidth     = Input(UInt(Mem.X.getWidth.W))
  val writeData     = Input(UInt(WORD_WIDTH.W))

  val readData      = Output(UInt(WORD_WIDTH.W))  
}

class MusvitRAM(config: MusvitConfig) extends Module with OpCodes {
  val io = IO(new MusvitRAMIO(config))

  // Addresses
  val byteOffset = io.addr(log2Up(BYTES_PER_WORD) - 1, 0)
  val addr = (io.addr - config.ramAddr.U)(io.addr.getWidth - 1, byteOffset.getWidth) // TODO: maybe find another solution to this

  // Error checking
  assert((byteOffset === 0.U || io.dataWidth =/= Mem.W) || !io.en, "Word RAM operation not correctly aligned")
  assert((byteOffset(0) === 0.U || io.dataWidth =/= Mem.H) || !io.en, "Halfword RAM operation not correctly aligned")

  val mem = SyncReadMem(config.ramSize / BYTES_PER_WORD, Vec(BYTES_PER_WORD, UInt(BYTE_WIDTH.W)))
  
  val writeData = BarrelShifter.rightShift(BitsToByteVec(io.writeData), byteOffset)

  val maskTable = TruthTable(Map(
    (Mem.B ## BitPat("b00")) -> BitPat("b0001"),
    (Mem.B ## BitPat("b01")) -> BitPat("b0010"),
    (Mem.B ## BitPat("b10")) -> BitPat("b0100"),
    (Mem.B ## BitPat("b11")) -> BitPat("b1000"),
    (Mem.H ## BitPat("b00")) -> BitPat("b0011"),
    (Mem.H ## BitPat("b10")) -> BitPat("b1100"),
    (Mem.W ## BitPat("b00")) -> BitPat("b1111"),
  ),
    BitPat("b????")
  )

  val maskVec = VecInit(decoder(EspressoMinimizer, io.dataWidth ## byteOffset, maskTable).asBools)
  
  when (io.en && io.isWrite) {
    mem.write(addr, writeData, maskVec.toSeq)
  }
  
  // Reading
  val readData = mem.read(addr, io.en && !io.isWrite)
  val byteOffsetReg = Reg(chiselTypeOf(byteOffset))
  byteOffsetReg := byteOffset
  val shifted = BarrelShifter.leftShift(readData, byteOffsetReg).asUInt
  val isSignedReg = RegNext(io.isSigned)
  val dataWidthReg = RegNext(io.dataWidth)
  when (RegNext(io.en && !io.isWrite)) {
    io.readData := MuxCase(shifted, Seq(
      (isSignedReg && dataWidthReg === Mem.B) -> SignExtend(shifted, BYTE_WIDTH - 1, WORD_WIDTH),
      (isSignedReg && dataWidthReg === Mem.H) -> SignExtend(shifted, HALF_WIDTH - 1, WORD_WIDTH),
      (!isSignedReg && dataWidthReg === Mem.B) -> shifted(BYTE_WIDTH - 1, 0),
      (!isSignedReg && dataWidthReg === Mem.H) -> shifted(HALF_WIDTH - 1, 0),
    ))
  }.otherwise {
    io.readData := 0.U // Maybe remove?
  }
}
