package musvit

import chisel3._
import circt.stage.ChiselStage

import memory.MusvitROM
import musvit.fetch.FetchPacket
import utility.Constants._
import utility.Functions._
import utility.OptionsParser
import musvit.execute.OperandSupplier
import io.MultiplexedSevenSegmentDriver

class MusvitIO(config: MusvitConfig) extends Bundle {
  val seg = UInt(7.W)
  val an = UInt(4.W)
}

class Musvit(config: MusvitConfig) extends Module {
  val io = IO(new MusvitIO(config))

  val rom = MusvitROM(config)
  val core = MusvitCore(config)

  core.io.read <> rom.io

  val sevSeg = Module(new MultiplexedSevenSegmentDriver(4, config.clockFrequency, true, true, true))
  sevSeg.io.value := core.io.printReg
  io.an := sevSeg.io.segSel
  io.seg := sevSeg.io.segment
}

object MusvitMain extends App {
  println("\n\nGenerating SystemVerilog")
  val options = OptionsParser.getOptions(args)
  OptionsParser.printOptions(args)
  ChiselStage.emitSystemVerilogFile(
    gen = new Musvit(options.musvitConfig),
    args = options.firrtlOpts,
    firtoolOpts = options.firtoolOpts
  )
  println("\n\n")
}
