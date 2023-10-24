package memory

import chisel3._
import chisel3.util.{log2Up, RegEnable, isPow2}

import utility.Functions._
import utility.Constants._
import utility.BarrelShifter
import musvit.MusvitConfig

object ROM {
  def apply(contents: Seq[UInt]) = {
    Module(new ROM(contents))
  }
}

class ROM(contents: Seq[UInt]) extends Module {
  val io = IO(new ReadIO(contents.length.toLong, contents.head.getWidth))

  val rom = VecInit(contents)

  val addrReg = RegEnable(io.addr, 0.U, io.en)

  io.data := rom(addrReg)
}

class MusvitROMIO(config: MusvitConfig) extends Bundle {
  val en = Input(Bool())
  val addr = Input(UInt(log2Up(config.romSize).W))
  val data = Output(Vec(config.fetchWidth, UInt(INST_WIDTH.W)))
}

class MusvitROM(config: MusvitConfig) extends Module {
  val io = IO(new MusvitROMIO(config))
  // val io = IO(new ReadIO(config.romSize, INST_WIDTH * config.fetchWidth))

  // Get ROM contents from file
  val contents = fileToUInts(config.romFile, INST_WIDTH)

  // Error checking
  if ((contents.length * (contents.head.getWidth / BYTE_WIDTH)) > config.romSize)
    throw new Error(
      "Contents of romFile does not fit in MusvitROM of size 0x%X".format(config.romSize)
    )
  if (!isPow2(config.fetchWidth))
    throw new Error("fetchWidth must be a power of 2")
  if (config.fetchWidth < 2)
    throw new Error("fetchWidth must be greater or equal to 2")

  // Split contents into multiple content sequences
  val contentsSeqs = contents.zipWithIndex
    .groupMap(_._2 % config.fetchWidth)(_._1)
    .toSeq
    .sortBy(_._1)
    .map(_._2)

  // Create ROMS
  val roms = Seq.tabulate(config.fetchWidth)((i) => VecInit(contentsSeqs(i)))

  // Addresses
  val byteOffset = io.addr(log2Up(BYTES_PER_INST) - 1, 0)
  val shamt      = io.addr(log2Up(config.fetchWidth) + byteOffset.getWidth - 1, byteOffset.getWidth)
  val addr       = io.addr(io.addr.getWidth - 1, shamt.getWidth + byteOffset.getWidth)

  // No support for byte indexing
  assert(byteOffset === 0.U, "Bytes indexing in MusvitROM is not supported")
  dontTouch(byteOffset)
  
  //val shamtReg = RegEnable(shamt, 0.U(log2Up(config.fetchWidth).W), io.en)
  val increTable = VecInit.tabulate(
    config.fetchWidth, config.fetchWidth)((romIdx, i) => (romIdx < i).B.asUInt)

  // Increment addresses
  val data = roms.zipWithIndex.map { case (rom, i) =>
    rom(addr + increTable(i)(shamt))
  }

  // Rotate data
  io.data := BarrelShifter.leftRotate(VecInit(data), shamt)
}

object MusvitROM {
  def apply(config: MusvitConfig) = {
    Module(new MusvitROM(config))
  }
}
