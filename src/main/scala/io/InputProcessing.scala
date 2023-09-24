package io

import chisel3._
import chisel3.util.Counter

object SyncInput {
  def apply[T <: Data](signal: T): T = {
    RegNext(RegNext(signal))
  }
}

object DebounceInput {
  def apply[T <: Data](signal: T, counterMax: Int): T = {
    // Ignore reset for this signal
    val signalDebounced = Reg(chiselTypeOf(signal))
    val counterEnable = !(signalDebounced === signal)
    val (_, counterTick) = Counter(0 until counterMax - 1, counterEnable, !counterEnable)
    signalDebounced := Mux(counterTick, signal, signalDebounced)
    signalDebounced
  }
}

object HandleHumanInput {
  def apply[T <: Data](signal: T, clkFreg: Int): T = {
    DebounceInput(SyncInput(signal), clkFreg / 100)
  }
}
