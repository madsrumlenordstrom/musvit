package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import musvit.common.ControlValues
import utility.Constants._

class ReservationStationIO(config: MusvitConfig) extends Bundle with ControlValues {
  val ib = Flipped(Decoupled(IssueBus(config)))                           // Issue bus
  val cdb = Flipped(Vec(config.fetchWidth, Valid(CommonDataBus(config)))) // Commmon data bus for monitor
  val flush = Input(Bool())
}

class ReservationStation(config: MusvitConfig) extends Module {
  val rs = IO(new ReservationStationIO(config))

  // Busy indicator
  val busyReg = RegInit(false.B)
  rs.ib.ready := !busyReg

  // Source data
  val rsReg = RegInit(0.U.asTypeOf(IssueBus(config)))
  val dataValid = rsReg.src1.data.valid && rsReg.src2.data.valid

  when (rs.ib.fire) {
    rsReg := rs.ib.bits
    busyReg := true.B // Functional unit will be responsible for setting this low when operation is complete
  }

  def connectCDB(isReg: IssueSource): Unit = {
    // Look for valid data on common data bus
    rs.cdb.zipWithIndex.foreach { case (cdb, i) =>
      when (!isReg.data.valid && cdb.bits.robTag === isReg.robTag && cdb.valid) {
        isReg.data.bits := cdb.bits.data
        isReg.data.valid := true.B
      }
    }
  }
  connectCDB(rsReg.src1)
  connectCDB(rsReg.src2)

  // Flush by setting state to non busy
  when (rs.flush) {
    busyReg := false.B
  }
}

class TestingReservationStation(config: MusvitConfig) extends ReservationStation(config) {
  val debug = IO(Decoupled(IssueBus(config)))

  debug.valid := dataValid
  debug.bits <> rsReg
  
  // Mark operation as done
  when (debug.fire) {
    busyReg := false.B
  }
}