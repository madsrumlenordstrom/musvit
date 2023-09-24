package musvit

import chisel3._
import chisel3.util.{is, switch, MuxLookup}
import circt.stage.ChiselStage
import scala.sys._
import java.io.File
import utility.OptionsParser
import utility.Functions.{fileToByteSeq}
import utility.RisingEdge
import io.{MultiplexedSevenSegmentDriver}
import io.HandleHumanInput

class Musvit(romContent: File, clockFrequency: Int) extends Module {
  val io = IO(new Bundle {
    val autoCount = Input(Bool())
    val showUpper = Input(Bool())
    val increment = Input(Bool())
    val decrement = Input(Bool())
    val switchDis = Input(Bool())
    val leds = Output(UInt(16.W))
    val seg = Output(UInt(7.W))
    val an = Output(UInt(4.W))
  })
  val cntReg = RegInit(0.U(32.W))
  val CNT_MAX = (clockFrequency / 2 - 1).U
  val idx = RegInit(0.U(32.W))

  val rom = VecInit(fileToByteSeq(romContent))
  val data = rom(idx + 3.U) ## rom(idx + 2.U) ## rom(idx + 1.U) ## rom(idx)

  when(io.autoCount) {
    cntReg := cntReg + 1.U
  }

  when(cntReg === CNT_MAX) {
    cntReg := 0.U
    idx := idx + 4.U
  }

  when(RisingEdge(HandleHumanInput(io.increment, clockFrequency))) {
    cntReg := 0.U
    idx := idx + 4.U
  }.elsewhen(RisingEdge(HandleHumanInput(io.decrement, clockFrequency))) {
    cntReg := 0.U
    idx := idx - 4.U
  }

  // Output
  val dispMux = Module(new MultiplexedSevenSegmentDriver(4, clockFrequency, true, true, false))
  val dispValue = Wire(UInt(32.W))
  dispValue := Mux(io.switchDis, idx, data)
  dispMux.io.value := Mux(io.showUpper, dispValue(31, 16), dispValue(15, 0))
  io.an := dispMux.io.segSel
  io.seg := dispMux.io.segment

  val ledsValue = Wire(UInt(32.W))
  ledsValue := Mux(io.switchDis, data, idx)
  io.leds := Mux(io.showUpper, ledsValue(31, 16), ledsValue(15, 0))
}

object MusvitMain extends App {
  println("\n\nGenerating SystemVerilog")
  val options = OptionsParser.getOptions(args)
  OptionsParser.printOptions(args)
  ChiselStage.emitSystemVerilogFile(
    gen = new Musvit(romContent = options.romContents, clockFrequency = options.clockFrequency),
    args = options.firrtlOpts,
    firtoolOpts = options.firtoolOpts
  )
  println("\n\n")
}
