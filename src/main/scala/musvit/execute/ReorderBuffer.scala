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
  //val flush = Output(Bool())
  val issue = Flipped(Decoupled(Vec(config.fetchWidth, new ReorderBufferEntry(config))))
  val commit = Decoupled(Vec(config.fetchWidth, CommitBus(config)))
  val read = Vec(config.fetchWidth, new ReorderBufferReadPort(config))
  val cdb = Vec(config.fetchWidth, Flipped(Valid(CommonDataBus(config))))
}

class ReorderBuffer(config: MusvitConfig) extends Module with ControlSignals {
  val io = IO(new ReorderBufferIO(config))

  val entries = config.robEntries / config.fetchWidth
  val enq_ptr = Counter(entries)
  val deq_ptr = Counter(entries)
  val maybe_full = RegInit(false.B)
  val ptr_match = enq_ptr.value === deq_ptr.value
  val empty = ptr_match && !maybe_full
  val full = ptr_match && maybe_full
  val do_enq = WireDefault(io.issue.fire)
  val do_deq = WireDefault(io.commit.fire)
  val flush = false.B

  val rob = Reg(Vec(entries, Vec(config.fetchWidth, new ReorderBufferEntry(config))))
  val robAddrWidth = ROBTag(config).getWidth - log2Up(config.fetchWidth)
  
  def robTagToRobEntry(robTag: UInt): ReorderBufferEntry = {
    // | Index into ROB           | Index into Vec returned by ROB
    rob(robTag.head(robAddrWidth))(robTag.tail(robAddrWidth))
  }

  when(do_enq) {
    rob(enq_ptr.value) := io.issue.bits
    enq_ptr.inc()
  }
  // Dequeue only when all entrues are ready
  val dequeueReady = rob(deq_ptr.value).map(_.ready).reduce(_ && _)
  when(do_deq) {
    deq_ptr.inc()
  }
  when(do_enq =/= do_deq) {
    maybe_full := do_enq
  }
  when(flush) {
    enq_ptr.reset()
    deq_ptr.reset()
    maybe_full := false.B
  }

  io.issue.ready := !full
  io.commit.valid := !empty

  io.commit.bits.zipWithIndex.foreach{ case (cb, i) => cb.:=(rob(deq_ptr.value)(i).commit) }

  // Read operands
  for (i <- 0 until io.read.length) {
    io.read(i).data1 := robTagToRobEntry(io.read(i).robTag1).commit.data
    io.read(i).data2 := robTagToRobEntry(io.read(i).robTag2).commit.data
  }

  // Write results from CDB
  for (i <- 0 until io.cdb.length) {
    when(io.cdb(i).valid) {
      robTagToRobEntry(io.cdb(i).bits.tag).commit.data := io.cdb(i).bits.data
      robTagToRobEntry(io.cdb(i).bits.tag).ready := true.B
    }
  }
}
