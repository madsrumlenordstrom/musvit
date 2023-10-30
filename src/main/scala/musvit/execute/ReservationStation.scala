package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import musvit.common.OpCodes
import utility.Constants._

class ReservationStationIO(config: MusvitConfig) extends Bundle with OpCodes {
  val ib = Vec(config.fetchWidth, Input(IssueBus(config)))                          // Issue bus
  val cdb = Flipped(Vec(config.fetchWidth, Decoupled(CommonDataBus(config))))       // Commmon data bus
  val fu = Decoupled(new FunctionalUnitOperands(config))                                    // Functional unit
}

class ReservationStation(config: MusvitConfig, val tag: UInt) extends Module {
  val io = IO(new ReservationStationIO(config))

  val busyReg = RegInit(false.B)
  val rsReg = RegInit(0.U.asTypeOf(new IssueBusFields(config)))
  val dataValid = rsReg.fields.map(_.tag === tag)

  // Get data from issue bus
  io.ib.zipWithIndex.foreach{ case (ib, i) => 
    when(io.ib(i).tag === tag && !busyReg) {
      rsReg := io.ib(i).data
      busyReg := true.B
    }
  }

  // Look for valid data on common data bus
  io.cdb.zipWithIndex.foreach { case (cdb, i) =>
    cdb.ready := false.B
    rsReg.fields.zipWithIndex.foreach{ case (fields, j) =>
      when(!dataValid(j) && cdb.bits.tag === fields.tag && cdb.valid) {
        rsReg.fields(j).data := cdb.bits.data
        rsReg.fields(j).tag := tag
        cdb.ready := true.B // not really used
      }  
    }

    // Check if result has been written
    when (busyReg && cdb.valid && cdb.bits.tag === tag) {
      busyReg := false.B
    }
  }

  io.fu.valid := VecInit(dataValid).asUInt.andR
  io.fu.bits.op := rsReg.op
  io.fu.bits.data1 := rsReg.fields(0).data
  io.fu.bits.data2 := rsReg.fields(1).data
}