package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import utility.Constants._
import musvit.common.ControlValues

object ROBTag {
  def apply(config: MusvitConfig): UInt = {
    UInt(log2Up(config.robEntries).W) // TODO: figure out width
  }
}

class ROBTagged[T <: Data](gen: T, config: MusvitConfig) extends Bundle {
  val tag = ROBTag(config)
  val data = gen
}

object ROBTagged {
  def apply[T <: Data](gen: T, config: MusvitConfig): ROBTagged[T] = new ROBTagged(gen, config)
}

object CommonDataBus {
  def apply(config: MusvitConfig) = {
    ROBTagged(UInt(WORD_WIDTH.W), config)
  }
}

class IssueSource(config: MusvitConfig) extends Bundle {
  val data = Valid(UInt(WORD_WIDTH.W))
  val tag = ROBTag(config)
}

class IssueBus(config: MusvitConfig) extends Bundle with ControlValues {
  val op = UInt(OP_WIDTH.W)
  val src1 = new IssueSource(config)
  val src2 = new IssueSource(config)
  val robTag = ROBTag(config)
  val imm = UInt(WORD_WIDTH.W)
}

object IssueBus {
  def apply(config: MusvitConfig): IssueBus = {
    new IssueBus(config)
  }
}

class CommitBus(config: MusvitConfig) extends Bundle with ControlValues {
  val data    = UInt(WORD_WIDTH.W)      // Value to write to RF or Mem
  val target  = UInt(ADDR_WIDTH.W)      // PC target for branches and jumps
  val rd      = UInt(REG_ADDR_WIDTH.W)  // Destination register
  val wb      = UInt(WB.X.getWidth.W)   // Writeback types
}

object CommitBus {
  def apply(config: MusvitConfig): CommitBus = {
    new CommitBus(config)
  }
}


