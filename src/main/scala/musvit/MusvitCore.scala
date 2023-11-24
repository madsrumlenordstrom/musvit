package musvit

import chisel3._
import chisel3.util._

import musvit.fetch.Frontend
import musvit.execute.Backend
import memory.MusvitROMIO
import utility.Constants._

class MusvitCoreIO(config: MusvitConfig) extends Bundle {
  val read = Flipped(new MusvitROMIO(config))
  val exit = Output(Bool())
}

class MusvitCore(config: MusvitConfig) extends Module {
  val io = IO(new MusvitCoreIO(config))

  val frontend = Frontend(config)
  val backend = Backend(config)

  frontend.io.read <> io.read
  frontend.io.pc <> backend.io.pc
  frontend.io.flush <> backend.io.flush
  backend.io.mop <> frontend.io.mop

  io.exit := backend.io.exit
}

object MusvitCore {
  def apply(config: MusvitConfig) = {
    Module(new MusvitCore(config))
  }
}