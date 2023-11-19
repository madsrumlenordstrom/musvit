package utility

object Constants {
  val WORD_WIDTH = 32
  val HALF_WIDTH = 16
  val BYTE_WIDTH = 8
  
  val INST_WIDTH = 32
  val ADDR_WIDTH = 32

  val BYTES_PER_INST = INST_WIDTH / BYTE_WIDTH
  val BYTES_PER_WORD = WORD_WIDTH / BYTE_WIDTH
  val BYTES_PER_HALF = HALF_WIDTH / BYTE_WIDTH
  val BYTES_PER_BYTE = BYTE_WIDTH / BYTE_WIDTH

  // Abstract out magic numbers for RISC-V ISA
  val NUM_OF_REGS = 32
  val REG_ADDR_WIDTH = 5
  val RD_LSB  = 7
  val RD_MSB  = 11
  val RS1_LSB = 15
  val RS1_MSB = 19
  val RS2_LSB = 20
  val RS2_MSB = 24
}