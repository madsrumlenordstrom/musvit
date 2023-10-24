package memory

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants._
import utility.Functions._
import musvit.MusvitConfig
import musvit.common.OpCodes
import chisel3.util.is

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

      def write(addr: UInt, data: UInt, dataWidth: UInt, steps: Int = 1): Unit = {
        dut.io.en.poke(true.B)
        dut.io.isWrite.poke(true.B)
        dut.io.dataWidth.poke(dataWidth)
        dut.io.addr.poke(addr)
        dut.io.writeData.poke(data)
        dut.clock.step(steps)
      }

      def read(addr: UInt, expected: UInt, dataWidth: UInt, isSigned: Bool, steps: Int = 1): Unit = {
        dut.io.en.poke(true.B)
        dut.io.isWrite.poke(false.B)
        dut.io.isSigned.poke(isSigned)
        dut.io.addr.poke(addr)
        dut.io.dataWidth.poke(dataWidth)
        dut.clock.step(steps)
        dut.io.readData.expect(expected)
      }

      def writeRAM(data: Seq[UInt], dataWidth: UInt, width: Int): Unit = {
        for (i <- 0 until data.length) {
          write((i * (width / BYTE_WIDTH)).asUInt(dut.io.addr.getWidth.W), data(i), dataWidth)
        }      
      }

      def readRAM(data: Seq[UInt], dataWidth: UInt, width: Int): Unit = {
        for (i <- 0 until data.length) {
          read((i * (width / BYTE_WIDTH)).asUInt(dut.io.addr.getWidth.W), data(i), dataWidth, false.B)
        }
      }

      def checkRAM(data: Seq[UInt], dataWidth: UInt, width: Int): Unit = {
        writeRAM(data, dataWidth, width)
        readRAM(data, dataWidth, width)
      }

      dut.clock.setTimeout(0)

      println("Checking bytes")
      checkRAM(bytes, "b01".U, BYTE_WIDTH)
      println("Checking halfs")
      checkRAM(halfs, "b10".U, HALF_WIDTH)
      println("Checking words")
      checkRAM(words, "b00".U, WORD_WIDTH)

      println("Checking sign extension")
      write(0.U, "x80".U, "b01".U)
      read(0.U,  "xFFFFFF80".U, "b01".U, true.B)
      write(0.U, "x80".U, "b01".U)
      read(0.U,  "x00000080".U, "b01".U, false.B)
      write(0.U, "x8320".U, "b10".U)
      read(0.U,  "xFFFF8320".U, "b10".U, true.B)
      write(0.U, "x8320".U, "b10".U)
      read(0.U,  "x00008320".U, "b10".U, false.B)
    }
  }
}