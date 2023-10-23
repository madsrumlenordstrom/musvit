package memory

import chisel3._
import chisel3.util.log2Up

import musvit.MusvitConfig
import utility.Constants._
import utility.BitsToByteVec

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

class MusvitRAMIO(config: MusvitConfig) extends Bundle {
  val addr          = Input(UInt(log2Up(config.ramSize).W))
  val en            = Input(Bool())
  val isWrite       = Input(Bool())
  val isSigned      = Input(Bool())

  val writeData     = Input(UInt(DATA_WIDTH.W))
  val readData      = Output(UInt(DATA_WIDTH.W))  
  
}

class MusvitRAM(config: MusvitConfig) extends Module {
  val io = IO(new MusvitRAMIO(config))

  // Addresses
  val byteOffset = io.addr(log2Up(BYTES_PER_DATA) - 1, 0)

  val mem = SyncReadMem(config.ramSize / BYTES_PER_DATA, Vec(BYTES_PER_DATA, UInt(BYTE_WIDTH.W)))

  val addr = (io.addr - config.ramAddr.U)(io.addr.getWidth - 1, byteOffset.getWidth) // TODO: maybe find another solution to this

  val dataVec = BitsToByteVec(io.writeData)

  val readData = mem.readWrite(addr, dataVec, io.en, io.isWrite)

}
