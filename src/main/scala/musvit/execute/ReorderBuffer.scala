package musvit.execute

import chisel3._
import chisel3.util._

import utility.Constants._
import musvit.MusvitConfig
import musvit.common.ControlValues

class ReorderBufferReadPort(config: MusvitConfig) extends Bundle {
  val robTag1 = Input(ROBTag(config))
  val robTag2 = Input(ROBTag(config))
  val data1 = Valid(UInt(WORD_WIDTH.W))
  val data2 = Valid(UInt(WORD_WIDTH.W))
}

class ReorderBufferIO(config: MusvitConfig) extends Bundle {
  val flush = Input(Bool())
  val issue = Flipped(Decoupled(Vec(config.fetchWidth, CommitBus(config))))
  val commit = Decoupled(Vec(config.fetchWidth, CommitBus(config)))
  val read = Vec(config.fetchWidth, new ReorderBufferReadPort(config))
  val cdb = Vec(config.fetchWidth, Flipped(Valid(CommonDataBus(config))))
}

class ReorderBuffer(config: MusvitConfig) extends Module with ControlValues {
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

  val rob = Reg(Vec(entries, Vec(config.fetchWidth, CommitBus(config))))
  val robAddrWidth = ROBTag(config).getWidth - log2Up(config.fetchWidth)
  val readyReg = Reg(Vec(entries, Vec(config.fetchWidth, Bool())))
  val dequeueReady = readyReg(deq_ptr.value).reduce(_ && _)

  def robTagToRobEntry(robTag: UInt): CommitBus = {
    // | Index into ROB           | Index into Vec returned by ROB
    rob(robTag.head(robAddrWidth))(robTag.tail(robAddrWidth))
  }

  def robTagToReadyEntry(robTag: UInt): Bool = {
    //       | Index into REG           | Index into Vec returned
    readyReg(robTag.head(robAddrWidth))(robTag.tail(robAddrWidth))
  }

  when(do_enq) {
    rob(enq_ptr.value) := io.issue.bits
    readyReg(enq_ptr.value) := VecInit.fill(config.fetchWidth)(false.B)
    enq_ptr.inc()
  }
  when(do_deq) {
    deq_ptr.inc()
  }
  when(do_enq =/= do_deq) {
    maybe_full := do_enq
  }
  when(io.flush) {
    enq_ptr.reset()
    deq_ptr.reset()
    maybe_full := false.B
  }

  io.issue.ready := !full
  io.commit.valid := !empty && dequeueReady

  io.commit.bits <> rob(deq_ptr.value)

  // Read operands
  for (i <- 0 until io.read.length) {
    io.read(i).data1.bits   := robTagToRobEntry(io.read(i).robTag1).data
    io.read(i).data1.valid  := robTagToReadyEntry(io.read(i).robTag1)
    io.read(i).data2.bits   := robTagToRobEntry(io.read(i).robTag2).data
    io.read(i).data2.valid  := robTagToReadyEntry(io.read(i).robTag2)
  }

  // Write results from CDB
  for (i <- 0 until io.cdb.length) {
    when(io.cdb(i).valid && !robTagToReadyEntry(io.cdb(i).bits.tag)) {
      robTagToRobEntry(io.cdb(i).bits.tag).data <> io.cdb(i).bits.data
      robTagToReadyEntry(io.cdb(i).bits.tag) := true.B
    }
  }
}
