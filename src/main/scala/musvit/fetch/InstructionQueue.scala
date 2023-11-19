package musvit.fetch


import chisel3._
import chisel3.util._

import musvit.MusvitConfig

class InstructionQueueIO(config: MusvitConfig) extends Bundle {
  val in = Flipped(Decoupled(new FetchPacket(config)))
  val out = Decoupled(new FetchPacket(config))
  val flush = Input(Bool())
}

class InstructionQueue(config: MusvitConfig) extends Module {
  val io = IO(new InstructionQueueIO(config))

  val queue = Module(new Queue(new FetchPacket(config), config.instQueueEntries / config.fetchWidth, hasFlush = true))

  queue.io.enq <> io.in
  io.out <> queue.io.deq
  queue.io.flush.get := io.flush
}
