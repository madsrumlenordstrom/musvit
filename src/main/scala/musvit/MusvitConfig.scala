package musvit

case class MusvitConfig(
    fetchWidth: Int = 2,
    resetPC: Long = 0L,
    romFile: String = "", // Rename to romFile or similar
    clockFrequency: Int = -1,
)
