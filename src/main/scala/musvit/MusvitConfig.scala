package musvit

import chisel3._

case class MusvitConfig(
    issueWidth: Int = 2,         // Number of instructions issued per cycle
    instQueueEntries: Int = 8,   // Number of entries in instruction queue
    robEntries: Int = 8,         // Number of entries in ROB
    btbEntries: Int = 8,         // Number of entries in BTB
    aluNum: Int = 2,             // Number of ALU units
    mulNum: Int = 1,             // Number of multiply units
    divNum: Int = 1,             // Number of division units
    lsuNum: Int = 2,             // Number of load store units
    clockFrequency: Int = -1,    // Clock frequency of Musvit
    romFile: String = "",        // File with ROM contents
    romSize: Int = 0x00001000,   // ROM size in bytes
    romAddr: Long = 0x00000000L, // Start address of ROM
    resetPC: Long = 0x00000000L, // Initial PC value
    ramSize: Int = 0x00001000,   // RAM size in bytes
    ramAddr: Long = 0x00001000L, // Start address of RAM
)

// TODO use BigInt instead of Int and Long for memory related stuff

object MusvitConfig {
    val tiny   = MusvitConfig(issueWidth = 1, robEntries = 4,  btbEntries = 4,  instQueueEntries = 4,  aluNum = 1, mulNum = 1, divNum = 1, romFile = "sw/build/fibonacci-fpga.bin", clockFrequency = 100000000)
    val small  = MusvitConfig(issueWidth = 2, robEntries = 8,  btbEntries = 8,  instQueueEntries = 8,  aluNum = 2, mulNum = 1, divNum = 1, romFile = "sw/build/fibonacci-fpga.bin", clockFrequency = 100000000)
    val medium = MusvitConfig(issueWidth = 2, robEntries = 16, btbEntries = 16, instQueueEntries = 16, aluNum = 4, mulNum = 2, divNum = 2, romFile = "sw/build/fibonacci-fpga.bin", clockFrequency = 100000000)
    val large  = MusvitConfig(issueWidth = 4, robEntries = 16, btbEntries = 16, instQueueEntries = 16, aluNum = 4, mulNum = 2, divNum = 2, romFile = "sw/build/fibonacci-fpga.bin", clockFrequency = 100000000)
}