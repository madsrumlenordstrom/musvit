package musvit

case class MusvitConfig(
    fetchWidth: Int = 2,            // Number of instructions fetched per fetch
    romFile: String = "",           // File with ROM contents
    romSize: Int = 0x00001000,      // ROM size in bytes
    romAddr: Long = 0x00000000L,    // Start address of ROM
    resetPC: Long = 0x00000000L,    // Initial PC value
    clockFrequency: Int = -1,       // Clock frequency of Musvit
    ramSize: Int = 0x00001000,      // RAM size in bytes
    ramAddr: Long = 0x00001000L,    // Start address of RAM
)

// TODO use BigInt instead of Int and Long for memory related stuff

object MusvitConfigs {
    val basys3 = MusvitConfig(
        fetchWidth = 2,
        romFile = "sw/build/blinky.bin",
        clockFrequency = 100000000,
    )
}