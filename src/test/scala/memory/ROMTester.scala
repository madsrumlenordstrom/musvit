package memory

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import utility.Constants._
import utility.Functions._
import java.nio.file.Files
import java.io.File

class ROMTester extends AnyFlatSpec with ChiselScalatestTester {
  val testFile = "sw/build/add.bin"
  val width = INST_WIDTH * 2
  val bytes = fileToByteSeq(testFile)
  "ROM" should "pass" in {
    test(new ROM(bytes)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.en.poke(true.B)
      for (i <- 0 until dut.rom.length) {
        dut.io.addr.poke(i.U)
        dut.clock.step(1)
        println("0x" + i.toHexString + " " +  dut.io.data.peekInt().toString(16))
      }
    }
  }
}

class MusvitROMTester extends AnyFlatSpec with ChiselScalatestTester {
  val testFile = "sw/build/blinky.bin"
  val fetchWidth = 4
  val bytes = fileToByteSeq(testFile)
  val formatStr = "0x%08X %0" + (fetchWidth * 2) + "X"
  "MusvitROM" should "pass" in {
    test(new MusvitROM(bytes, fetchWidth)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.en.poke(true.B)
      for (i <- 0 until bytes.length by fetchWidth) {
        dut.io.addr.poke(i.U)
        dut.clock.step(1)
        println(formatStr.format(i, dut.io.data.peekInt()))
      }
    }
  }
}