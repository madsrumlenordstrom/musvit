package memory

import chisel3._
import chisel3.util.{log2Up, RegEnable}

import utility.Functions._
import utility.Constants._

import scala.collection.Iterator._

class ROMIO(size: Long, width: Int) extends Bundle {
  val readEn = Input(Bool())
  val readAddr = Input(UInt(log2Up(size).W))
  val readData = Output(UInt(width.W))
}

class ROM(width: Int, romContents: String) extends Module {
  // Get ROM data
  val wordSeq = fileToWordSeq(romContents, width, 0.U(BYTE_WIDTH.W))

  val io = IO(new ROMIO(wordSeq.length.toLong, width))

  val rom = VecInit(wordSeq)

  val addrReg = RegEnable(io.readAddr, 0.U, io.readEn)

  io.readData := rom(addrReg)
}
