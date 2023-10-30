package musvit

import chisel3._
import circt.stage.ChiselStage

import memory.MusvitROM
import musvit.fetch.FetchPacket
import utility.Constants._
import utility.Functions._
import utility.OptionsParser

class MusvitIO(config: MusvitConfig) extends Bundle {
  val temp = Output(new FetchPacket(config))
}

class Musvit(config: MusvitConfig) extends Module {
  val io = IO(new MusvitIO(config))

  val rom = MusvitROM(config)
  val musvitCore = MusvitCore(config)

  rom.io <> musvitCore.io.instMem
  
  io.temp := musvitCore.io.temp
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