package memory

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import utility.Constants._
import utility.Functions._
import musvit.MusvitConfig

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

class MusvitRAMTester extends AnyFlatSpec with ChiselScalatestTester {

  // Test configuration
  val testFile = "random"
  val width = BYTE_WIDTH
  val words = fileToUInts(testFile, width)
  val config = MusvitConfig()

  "SyncRAM" should "pass" in {
    test(new MusvitRAM(config)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>

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