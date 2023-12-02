package musvit.execute

import chisel3._
import chisel3.util._

import musvit.MusvitConfig
import utility.Constants._
import musvit.common.ControlValues

class RegisterFileReadPort(config: MusvitConfig) extends Bundle {
  val rs1 = Input(UInt(REG_ADDR_WIDTH.W))
  val rs2 = Input(UInt(REG_ADDR_WIDTH.W))
  val data1 = Output(UInt(WORD_WIDTH.W))
  val data2 = Output(UInt(WORD_WIDTH.W))
}

class RegisterFileWritePort(config: MusvitConfig) extends Bundle {
  val rd = Input(UInt(REG_ADDR_WIDTH.W))
  val data = Input(UInt(WORD_WIDTH.W))
  val en = Input(Bool())
}

class RegisterFileIO(config: MusvitConfig) extends Bundle {
  val read = Vec(config.issueWidth, new RegisterFileReadPort(config))
  val write = Vec(config.issueWidth, new RegisterFileWritePort(config))
  val ecall = Input(Bool())
  val exit  = Output(Bool())
  val printReg = Output(UInt(WORD_WIDTH.W))
}

class RegisterFile(config: MusvitConfig) extends Module with ControlValues {
  val io = IO(new RegisterFileIO(config))

  val rf = Reg(Vec(NUM_OF_REGS, UInt(WORD_WIDTH.W)))

  for (i <- 0 until config.issueWidth) {
    io.read(i).data1 := rf(io.read(i).rs1)
    io.read(i).data2 := rf(io.read(i).rs2)

    when (io.write(i).en ) {
      rf(io.write(i).rd) := io.write(i).data
    }
  }

  rf(0) := 0.U // Hardwire to zero

  // ECALL stuff

  def printRegs() = {
    for(i <- 0 until 8){
      for(j <- 0 until 4){
        printf("x(" + (j*8 + i) + ")")
        if(j*8 + i == 8 || j*8 + i == 9){
          printf(" ")
        }
        printf(p" = 0x${Hexadecimal(rf(j*8 + i))}\t")
      }
      printf("\n")
    }
    printf("\n\n")
  }

  val a0 = rf(10)
  val a7 = rf(17)
  val printReg = RegInit(0.U(WORD_WIDTH.W))
  io.printReg := printReg

  io.exit := false.B

  when(io.ecall) {
    switch(a7) {
      is (0.U) {
        printf(p"${Decimal(a0)}\n")
        printReg := a0
      }
      is (3.U) {
        printRegs()
      }
      is (4.U) {
        printf(p"${Character(a0)}\n")
      }
      is (5.U) {
        printf(p"${Hexadecimal(a0)}\n")
      }
      is (6.U) {
        printf(p"${Binary(a0)}\n")
      }
      is (10.U) {
        printf("Exit\n")
        io.exit := true.B
      }
    }
  }
}