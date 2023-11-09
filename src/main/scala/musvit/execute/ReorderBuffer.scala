package musvit.execute

import chisel3._
import chisel3.util._

import utility.Constants._
import musvit.MusvitConfig
import musvit.common.ControlSignals

class ReorderBufferEntry(config: MusvitConfig) extends Bundle {
  val commit = CommitBus(config)
  val ready = Bool()
}

class ReorderBufferReadPort(config: MusvitConfig) extends Bundle {
  val robTag1 = Input(ROBTag(config))
  val robTag2 = Input(ROBTag(config))
  val data1 = Output(UInt(WORD_WIDTH.W))
  val data2 = Output(UInt(WORD_WIDTH.W))
}

class ReorderBufferIO(config: MusvitConfig) extends Bundle {
  val full = Output(Bool())
  //val flush = Output(Bool())
  val issue = Decoupled(Vec(config.fetchWidth, new ReorderBufferEntry(config)))
  val commit = Flipped(Decoupled(Vec(config.fetchWidth, CommitBus(config))))
  val read = Vec(config.fetchWidth, new ReorderBufferReadPort(config))
  val cdb = Vec(config.fetchWidth, Valid(CommonDataBus(config)))
}

class ReorderBuffer(config: MusvitConfig) extends Module with ControlSignals {
  val io = IO(new ReorderBufferIO(config))

  val rob = Module(
    new Queue(
      gen = Vec(config.fetchWidth, new ReorderBufferEntry(config)),
      entries = config.robEntries / config.fetchWidth,
      pipe = false,
      flow = false,
      useSyncReadMem = false,
      hasFlush = true
    )
  )

  val robAddrWidth = ROBTag(config).getWidth - log2Up(config.fetchWidth)

  io.full := rob.full

  // Enqueue data from issue
  rob.io.enq <> io.issue

  // Dequeue data to commit
  val dequeueReady = rob.io.deq.bits.map(_.ready).reduce(_ && _)
  rob.io.deq.ready := dequeueReady && !rob.empty // empty maybe not needed

  // Connect commit bus
  io.commit.bits.zipWithIndex.foreach { case (commit, i) => commit.:=(rob.io.deq.bits(i).commit) }
  io.commit.valid := dequeueReady

  for (i <- 0 until io.commit.bits.length) {}

  // Read operands
  for (i <- 0 until io.read.length) {
    //                              | Index into ROB                       | Index into Vec returned by ROB
    io.read(i)
      .data1 := rob.ram.read(io.read(i).robTag1.head(robAddrWidth))(io.read(i).robTag1.tail(robAddrWidth)).commit.data
    io.read(i)
      .data2 := rob.ram.read(io.read(i).robTag2.head(robAddrWidth))(io.read(i).robTag2.tail(robAddrWidth)).commit.data
  }

  // Write results from CDB
  for (i <- 0 until io.cdb.length) {
    when(io.cdb(i).valid) {
      //     | Index into ROB                       | Index into Vec returned by ROB
      rob.ram(io.cdb(i).bits.tag.head(robAddrWidth))(io.cdb(i).bits.tag.tail(robAddrWidth)).commit.data := io
        .cdb(i)
        .bits
        .data
      rob.ram(io.cdb(i).bits.tag.head(robAddrWidth))(io.cdb(i).bits.tag.tail(robAddrWidth)).ready := true.B
    }
  }

}
