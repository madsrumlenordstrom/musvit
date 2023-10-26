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
  val op            = Input(UInt(OP_WIDTH.W))
  val writeData     = Input(UInt(WORD_WIDTH.W))

  val readData      = Output(UInt(WORD_WIDTH.W))  
}

class MusvitRAM(config: MusvitConfig) extends Module with OpCodes {
  val io = IO(new MusvitRAMIO(config))

  // Addresses
  val byteOffset = io.addr(log2Up(BYTES_PER_WORD) - 1, 0)
  val addr = (io.addr - config.ramAddr.U)(io.addr.getWidth - 1, byteOffset.getWidth) // TODO: maybe find another solution to this

  // Error checking
  assert((byteOffset === 0.U || io.op =/= Mem.WORD) || !io.en, "Word RAM operation not correctly aligned")
  assert((byteOffset(0) === 0.U || io.op =/= Mem.HALF) || !io.en, "Halfword RAM operation not correctly aligned")

  val mem = SyncReadMem(config.ramSize / BYTES_PER_WORD, Vec(BYTES_PER_WORD, UInt(BYTE_WIDTH.W)))
  
  val writeData = BarrelShifter.rightShift(BitsToByteVec(io.writeData), byteOffset)

  val maskTable = TruthTable(Map(
    (Mem.BYTE ## BitPat("b00")) -> BitPat("b0001"),
    (Mem.BYTE ## BitPat("b01")) -> BitPat("b0010"),
    (Mem.BYTE ## BitPat("b10")) -> BitPat("b0100"),
    (Mem.BYTE ## BitPat("b11")) -> BitPat("b1000"),
    (Mem.HALF ## BitPat("b00")) -> BitPat("b0011"),
    (Mem.HALF ## BitPat("b10")) -> BitPat("b1100"),
    (Mem.WORD ## BitPat("b00")) -> BitPat("b1111"),
  ),
    BitPat("b????")
  )

  val maskVec = VecInit(decoder(EspressoMinimizer, io.op ## byteOffset, maskTable).asBools)
  
  when (io.en && io.op === Mem.STORE) {
    mem.write(addr, writeData, maskVec.toSeq)
  }
  
  // Reading
  val readData = mem.read(addr, io.en && !(io.op === Mem.STORE))
  val byteOffsetReg = Reg(chiselTypeOf(byteOffset))
  byteOffsetReg := byteOffset
  val shifted = BarrelShifter.leftShift(readData, byteOffsetReg).asUInt
  val opReg = RegNext(io.op)
  //val isSignedReg = RegNext(io.isSigned)
  //val dataWidthReg = RegNext(io.dataWidth)
  
  when (RegNext(io.en && io.op === Mem.LOAD)) {
    io.readData := MuxCase(shifted, Seq(
      (opReg === Mem.LB) -> SignExtend(shifted, BYTE_WIDTH - 1, WORD_WIDTH),
      (opReg === Mem.LH) -> SignExtend(shifted, HALF_WIDTH - 1, WORD_WIDTH),
      (opReg === Mem.LBU) -> shifted(BYTE_WIDTH - 1, 0),
      (opReg === Mem.LHU) -> shifted(HALF_WIDTH - 1, 0),
    ))
  }.otherwise {
    io.readData := 0.U // Maybe remove?
  }
}
