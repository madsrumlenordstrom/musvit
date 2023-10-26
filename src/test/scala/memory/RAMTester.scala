package memory

import chisel3._
import chisel3.util.BitPat
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants._
import utility.Functions._
import musvit.MusvitConfig
import musvit.common.OpCodes

class RAMTester extends AnyFlatSpec with ChiselScalatestTester {

  // Test configuration
  val testFile = "random"
  val width = INST_WIDTH
  val words = fileToUInts(testFile, width)

  "SyncRAM" should "pass" in {
    test(new RAM(words.length, width)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      
      def write(addr: UInt, data: UInt, steps: Int = 1): Unit = {
        dut.io.writeEn.poke(true.B)
        dut.io.writeAddr.poke(addr)
        dut.io.writeData.poke(data)
        dut.clock.step(steps)
      }

      def read(addr: UInt, expected: UInt, steps: Int = 1): Unit = {
        dut.io.readEn.poke(true.B)
        dut.io.readAddr.poke(addr)
        dut.clock.step(steps)
        dut.io.readData.expect(expected)
      }
      
      dut.clock.setTimeout(0)

      // Write to RAM
      println("Writing to RAM")
      for (i <- 0 until words.length) {
        write(i.asUInt(dut.io.writeAddr.getWidth.W), words(i))
      }
      
      // Read RAM
      println("Reading RAM")
      for (i <- 0 until words.length) {
        read(i.asUInt(dut.io.readAddr.getWidth.W), words(i))
      }

      // Read and write bypass checking
      println("Checking bypass")
      for (i <- 0 until words.length) {
        val addr = i.asUInt(dut.io.readAddr.getWidth.W)
        val word = words(words.length - 1 - i)
        write(addr, word, 0)
        read(addr, word, 1)
      }
      
      // Check data is still there
      println("Reading RAM")
      for (i <- 0 until words.length) {
        val addr = i.asUInt(dut.io.readAddr.getWidth.W)
        val word = words(words.length - 1 - i)
        read(addr, word)
      }
    }
  }
}

class MusvitRAMTester extends AnyFlatSpec with ChiselScalatestTester with OpCodes {

  // Test configuration
  val testFile = "random"
  val bytes = fileToUInts(testFile, BYTE_WIDTH)
  val halfs = fileToUInts(testFile, HALF_WIDTH)
  val words = fileToUInts(testFile, WORD_WIDTH)
  val config = MusvitConfig(ramAddr = 0x00000000, ramSize = 0x00002000)

  "MusvitRAM" should "pass" in {
    test(new MusvitRAM(config)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>

      def write(addr: UInt, data: UInt, op: BitPat, steps: Int = 1): Unit = {
        dut.io.en.poke(true.B)
        dut.io.op.poke(BitPat.bitPatToUInt(op))
        dut.io.addr.poke(addr)
        dut.io.writeData.poke(data)
        dut.clock.step(steps)
      }

      def read(addr: UInt, expected: UInt, op: BitPat, steps: Int = 1): Unit = {
        dut.io.en.poke(true.B)
        dut.io.op.poke(BitPat.bitPatToUInt(op))
        dut.io.addr.poke(addr)
        dut.clock.step(steps)
        dut.io.readData.expect(expected)
      }

      def writeRAM(data: Seq[UInt], op: BitPat, width: Int): Unit = {
        for (i <- 0 until data.length) {
          write((i * (width / BYTE_WIDTH)).asUInt(dut.io.addr.getWidth.W), data(i), op)
        }      
      }

      def readRAM(data: Seq[UInt], op: BitPat, width: Int): Unit = {
        for (i <- 0 until data.length) {
          read((i * (width / BYTE_WIDTH)).asUInt(dut.io.addr.getWidth.W), data(i), op)
        }
      }

      def checkRAM(data: Seq[UInt], opW: BitPat, opR: BitPat, width: Int): Unit = {
        writeRAM(data, opW, width)
        readRAM(data, opR, width)
      }

      dut.clock.setTimeout(0)

      println("Checking bytes")
      checkRAM(bytes, Mem.SB, Mem.LBU, BYTE_WIDTH)
      println("Checking halfs")
      checkRAM(halfs, Mem.SH, Mem.LHU, HALF_WIDTH)
      println("Checking words")
      checkRAM(words, Mem.SW, Mem.LW, WORD_WIDTH)

      println("Checking sign extension")
      write(0.U, "x80".U, Mem.SB)
      read(0.U,  "xFFFFFF80".U, Mem.LB)
      write(0.U, "x80".U, Mem.SB)
      read(0.U,  "x00000080".U, Mem.LBU)
      write(0.U, "x8320".U, Mem.SH)
      read(0.U,  "xFFFF8320".U, Mem.LH)
      write(0.U, "x8320".U, Mem.SH)
      read(0.U,  "x00008320".U, Mem.LHU)
    }
  }
}