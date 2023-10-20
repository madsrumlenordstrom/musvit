package memory

import chisel3._
import chisel3.util.{log2Up, RegEnable, isPow2}

import utility.Functions._
import utility.Constants._
import utility.BarrelShifter

object ROM {
  def apply(contents: Seq[UInt]) = {
    Module(new ROM(contents))
  }
}

class ROM(contents: Seq[UInt]) extends Module {
  // Get ROM data
  // val wordSeq = fileToWordSeq(romContents, width, 0.U(BYTE_WIDTH.W))

  val io = IO(new ReadIO(contents.length.toLong, contents.head.getWidth))

  val rom = VecInit(contents)

  val addrReg = RegEnable(io.addr, 0.U, io.en)

  io.data := rom(addrReg)
}

object MusvitROM {
  def apply(contents : Seq[UInt], fetchWidth: Int) = {
    Module(new MusvitROM(contents, fetchWidth))
  }
}

class MusvitROM(contents: Seq[UInt], fetchWidth: Int) extends Module {
  val io = IO(
    new ReadIO(contents.length.toLong, contents.head.getWidth * fetchWidth)
  )

  // Error checking
  if (!isPow2(fetchWidth)) throw new Error("fetchWidth must be a power of 2")
  if (fetchWidth < 2) throw new Error("fetchWidth must be greater than 2")

  // Split contents into multiple content sequences
  val contentsSeqs = contents.zipWithIndex.groupMap(_._2 % fetchWidth)(_._1).toSeq.sortBy(_._1).map(_._2)

  // Create ROMS
  val roms = Seq.tabulate(fetchWidth)( (i) => ROM(contentsSeqs(i)))

  val shamt = io.addr(log2Up(fetchWidth) - 1, 0)
  val shamtReg = RegEnable(shamt, 0.U(log2Up(fetchWidth).W), io.en)
  val baseAddr = io.addr(log2Up(contents.length) - 2, log2Up(fetchWidth))
  val increTable = VecInit.tabulate(fetchWidth, fetchWidth)( (romIdx, i) => (romIdx < i).B.asUInt)

  // Increment addresses
  roms.foreach(_.io.en.:=(io.en))
  roms.zipWithIndex.foreach{ case (rom, i) => rom.io.addr.:=( baseAddr + increTable(i)(shamt) ) }

  // Rotate data
  io.data := BarrelShifter.leftRotate(VecInit(roms.map(_.io.data)), shamtReg).asUInt
}
