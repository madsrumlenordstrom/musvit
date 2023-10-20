package memory

import chisel3._
import chisel3.util.log2Up

import utility.Constants._

class ReadIO(size: Long, width: Int) extends Bundle {
  val en = Input(Bool())
  val addr = Input(UInt(log2Up(size).W))
  val data = Output(UInt(width.W))
}

class WriteIO(size: Long, width: Int) extends Bundle {
  val en = Input(Bool())
  val mask = Input(Vec(width / BYTE_WIDTH, Bool()))
  val addr = Input(UInt(log2Up(size).W))
  val data = Input(UInt(width.W))
}

class MemoryIO(size: Long, width: Int) extends Bundle {
  val read = new ReadIO(size, width)
  val write = new WriteIO(size, width)
}
