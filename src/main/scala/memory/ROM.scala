package memory

import chisel3._
import chisel3.util.{log2Up, RegEnable, isPow2, Decoupled}

import musvit.MusvitConfig
import utility.Functions._
import utility.Constants._
import utility.BarrelShifter

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
  val addr = Input(UInt(ADDR_WIDTH.W))
  // Decoupled IO has ready which will functions as a read enable (not used in ROM but useful for a cache)
  val data = Decoupled(Vec(config.issueWidth, UInt(INST_WIDTH.W)))
}

class MusvitROM(config: MusvitConfig) extends Module {
  val io = IO(new MusvitROMIO(config))

  // Get ROM contents from file
  val contents = fileToUInts(config.romFile, INST_WIDTH)

  // Error checking
  if ((contents.length * (contents.head.getWidth / BYTE_WIDTH)) > config.romSize)
    throw new Error(
      "Contents of romFile does not fit in MusvitROM of size 0x%X".format(config.romSize)
    )
  if (!isPow2(config.issueWidth))
    throw new Error("issueWidth must be a power of 2")
  if (config.issueWidth < 2)
    throw new Error("issueWidth must be greater or equal to 2")

  // Split contents into multiple content sequences
  val contentsSeqs = contents.zipWithIndex
    .groupMap(_._2 % config.issueWidth)(_._1)
    .toSeq
    .sortBy(_._1)
    .map(_._2)

  // Create ROMS
  val roms = Seq.tabulate(config.issueWidth)((i) => VecInit(contentsSeqs(i)))

  // Addresses
  val relaAddr   = io.addr - config.romAddr.U
  val byteOffset = relaAddr(log2Up(BYTES_PER_INST) - 1, 0)
  val shamt      = relaAddr(log2Up(config.issueWidth) + byteOffset.getWidth - 1, byteOffset.getWidth)
  val addr       = relaAddr(relaAddr.getWidth - 1, shamt.getWidth + byteOffset.getWidth)

  // No support for byte indexing
  assert(byteOffset === 0.U, "Bytes indexing in MusvitROM is not supported")
  dontTouch(byteOffset)
  
  //val shamtReg = RegEnable(shamt, 0.U(log2Up(config.issueWidth).W), io.en)
  val increTable = VecInit.tabulate(
    config.issueWidth, config.issueWidth)((romIdx, i) => (romIdx < i).B.asUInt)

  // Increment addresses
  val data = roms.zipWithIndex.map { case (rom, i) =>
    rom(addr + increTable(i)(shamt))
  }

  // Rotate data
  io.data.bits := BarrelShifter.leftRotate(VecInit(data), shamt)

  // Validate data
  io.data.valid := Mux(relaAddr < config.romSize.U, true.B, false.B)
}

object MusvitROM {
  def apply(config: MusvitConfig) = {
    Module(new MusvitROM(config))
  }
}
