package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import musvit.common.OpCodes
import utility.Constants._

class ReservationStationIO(config: MusvitConfig) extends Bundle with OpCodes {
  val ib = Vec(config.fetchWidth, Input(IssueBus(config)))                          // Issue bus
  val cdb = Flipped(Vec(config.fetchWidth, Decoupled(CommonDataBus(config))))       // Commmon data bus
}

class ReservationStation(config: MusvitConfig, val tag: Int) extends Module {
  val rs = IO(new ReservationStationIO(config))

  val busyReg = RegInit(false.B)
  val rsReg = RegInit(0.U.asTypeOf(new IssueBusFields(config)))
  val dataValidVec = rsReg.fields.map(_.tag === tag.U)
  val dataValid = VecInit(dataValidVec).asUInt.andR

  // Get data from issue bus
  rs.ib.zipWithIndex.foreach{ case (ib, i) => 
    when(rs.ib(i).tag === tag.U && !busyReg) {
      rsReg := rs.ib(i).data
      busyReg := true.B // Functional unit will be responsible for setting this low
    }
  }

  // Look for valid data on common data bus
  rs.cdb.zipWithIndex.foreach { case (cdb, i) =>
    cdb.ready := false.B
    rsReg.fields.zipWithIndex.foreach{ case (fields, j) =>
      when(!dataValidVec(j) && cdb.bits.tag === fields.tag && cdb.valid) {
        rsReg.fields(j).data := cdb.bits.data
        rsReg.fields(j).tag := tag.U
        cdb.ready := true.B // not really used
      }  
    }
  }
}

class TestingReservationStation(config: MusvitConfig, tag: Int) extends ReservationStation(config, tag) {
  val debug = IO(Decoupled(new FunctionalUnitOperands(config)))

  debug.valid := dataValid
  debug.bits.op := rsReg.op
  debug.bits.data1 := rsReg.fields(0).data
  debug.bits.data2 := rsReg.fields(1).data
  
  // Mark operation as done
  when (debug.fire) {
    busyReg := false.B
  }
}