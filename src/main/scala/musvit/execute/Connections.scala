package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import utility.Constants._
import musvit.common.ControlValues

object ROBTag {
  def apply(config: MusvitConfig): UInt = {
    UInt(log2Up(config.robEntries).W)
  }
}

class CommonDataBus(config: MusvitConfig) extends Bundle {
  val data    = UInt(WORD_WIDTH.W)   // Value to write to RF or Mem
  val target  = UInt(ADDR_WIDTH.W) // PC target for branches and jumps
  val robTag  = ROBTag(config)     // Index in ROB
}

object CommonDataBus {
  def apply(config: MusvitConfig) = {
    new CommonDataBus(config)
  }
}

class IssueSource(config: MusvitConfig) extends Bundle {
  val data    = Valid(UInt(WORD_WIDTH.W))
  val robTag  = ROBTag(config)
}

class IssueBus(config: MusvitConfig) extends Bundle with ControlValues {
  val op      = UInt(OP_WIDTH.W)
  val src1    = new IssueSource(config)
  val src2    = new IssueSource(config)
  val robTag  = ROBTag(config)
  val imm     = UInt(WORD_WIDTH.W)
  val pc      = UInt(ADDR_WIDTH.W)
}

object IssueBus {
  def apply(config: MusvitConfig): IssueBus = {
    new IssueBus(config)
  }
}

class ReorderBufferIssueFields(config: MusvitConfig) extends Bundle with ControlValues {
  val rd        = UInt(REG_ADDR_WIDTH.W)  // Destination register
  val wb        = UInt(WB.X.getWidth.W)   // Writeback types
  val branched  = Bool()                  // Indicates if branch was taken
  val valid     = Bool()                  // Indicates that issue is valid
}

class ReorderBufferDataFields(config: MusvitConfig) extends Bundle {
  val result    = UInt(WORD_WIDTH.W)      // Value to write to RF or Mem
  val target    = UInt(ADDR_WIDTH.W)      // PC target for branches and jumps
  val valid     = Bool()                  // Indicates that data is valid
}

class ReorderBufferIssuePort(config: MusvitConfig) extends Bundle {
  val fields = Vec(config.issueWidth, new ReorderBufferIssueFields(config))
  val pc = UInt(ADDR_WIDTH.W)
}

class ReorderBufferEntry(config: MusvitConfig) extends Bundle {
  val fields = Vec(config.issueWidth, CommitBus(config))
  val pc     = UInt(ADDR_WIDTH.W)
}

class CommitBus(config: MusvitConfig) extends Bundle with ControlValues {
  val issue = new ReorderBufferIssueFields(config)
  val data  = new ReorderBufferDataFields(config)
}

object CommitBus {
  def apply(config: MusvitConfig): CommitBus = {
    new CommitBus(config)
  }
}


