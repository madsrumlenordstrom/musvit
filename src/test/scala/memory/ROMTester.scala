package memory

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import java.io.File

import utility.Constants._
import utility.Functions._
import musvit.MusvitConfig

class ROMTester extends AnyFlatSpec with ChiselScalatestTester {
  val testFile = "random"
  val width = INST_WIDTH
  val contents = fileToUInts(testFile, INST_WIDTH)

  "ROM" should "pass" in {
    test(new ROM(contents)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.en.poke(true.B)
      for (i <- 0 until contents.length) {
        dut.io.addr.poke(i.U)
        dut.clock.step(1)
        dut.io.data.expect(contents(i))
      }
    }
  }
}

class MusvitROMTester extends AnyFlatSpec with ChiselScalatestTester {
  val config = MusvitConfig(fetchWidth = 2, romFile = "random", romSize = 0x2000)
  
  "MusvitROM" should "pass" in {
    test(new MusvitROM(config)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.data.ready.poke(true.B)

      for (i <- 0 until (dut.contents.length - config.fetchWidth)) {

        val addr = (i * BYTES_PER_INST).asUInt(ADDR_WIDTH.W)
        dut.io.addr.poke(addr)
        
        for (j <- 0 until config.fetchWidth) {
          dut.io.data.bits(j).expect(dut.contents(i + j))
        }
        
        dut.clock.step(1)
      }
    }
  }
}