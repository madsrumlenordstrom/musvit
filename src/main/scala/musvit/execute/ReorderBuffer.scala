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
  val flush       = Input(Bool())
  val issue       = Flipped(Decoupled(new ReorderBufferIssuePort(config)))
  val commit      = Decoupled(new ReorderBufferEntry(config))
  val read        = Vec(config.issueWidth, new ReorderBufferReadPort(config))
  val cdb         = Vec(config.issueWidth, Flipped(Valid(CommonDataBus(config))))
  val issueTags   = Output(Vec(config.issueWidth, ROBTag(config)))
  val commitTags  = Output(Vec(config.issueWidth, ROBTag(config)))
}

class ReorderBuffer(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new ReorderBufferIO(config))

  val entries = config.robEntries / config.issueWidth
  val enq_ptr = Counter(entries)
  val deq_ptr = Counter(entries)
  val maybe_full = RegInit(false.B)
  val ptr_match = enq_ptr.value === deq_ptr.value
  val empty = ptr_match && !maybe_full
  val full = ptr_match && maybe_full
  val do_enq = WireDefault(io.issue.fire)
  val do_deq = WireDefault(io.commit.fire)

  val rob = Reg(Vec(entries, new ReorderBufferEntry(config)))
  val robAddrWidth = ROBTag(config).getWidth - log2Ceil(config.issueWidth)

  def robTagToRobField(robTag: UInt): CommitBus = {
    // | Index into ROB           | Index into Vec returned by ROB
    rob(robTag.head(robAddrWidth)).fields(robTag.tail(robAddrWidth))
  }

  when(do_enq) {
    for (i <- 0 until config.issueWidth) {
      rob(enq_ptr.value).fields(i).issue := io.issue.bits.fields(i)
      rob(enq_ptr.value).fields(i).data.valid := false.B
    }
    rob(enq_ptr.value).pc := io.issue.bits.pc
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
  io.commit.valid := !empty
  io.commit.bits <> rob(deq_ptr.value)

  for (i <- 0 until io.issueTags.length) {
    io.issueTags(i) := enq_ptr.value ## i.U(log2Ceil(io.issueTags.length).W)
  }

  for (i <- 0 until io.commitTags.length) {
    io.commitTags(i) := deq_ptr.value ## i.U(log2Ceil(io.commitTags.length).W)
  }

  // Read operands
  for (i <- 0 until io.read.length) {
    io.read(i).data1.bits   := robTagToRobField(io.read(i).robTag1).data.result
    io.read(i).data1.valid  := robTagToRobField(io.read(i).robTag1).data.valid
    io.read(i).data2.bits   := robTagToRobField(io.read(i).robTag2).data.result
    io.read(i).data2.valid  := robTagToRobField(io.read(i).robTag2).data.valid
  }

  // Write results from CDB
  for (i <- 0 until io.cdb.length) {
    when(io.cdb(i).valid) {
      robTagToRobField(io.cdb(i).bits.robTag).data.result <> io.cdb(i).bits.data
      robTagToRobField(io.cdb(i).bits.robTag).data.target <> io.cdb(i).bits.target
      robTagToRobField(io.cdb(i).bits.robTag).data.valid := true.B
    }
  }
}
