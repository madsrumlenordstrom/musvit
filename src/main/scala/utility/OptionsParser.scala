package utility

import scopt.OParser
import scala.sys.exit
import java.io.File
import os.PathError
import musvit.MusvitConfig
import java.nio.file.Files
import java.nio.file.Paths

case class Config (
  firrtlOpts:     Array[String] = Array(),
  firtoolOpts:    Array[String] = Array(),
  musvitConfig:   MusvitConfig = MusvitConfig(),
)

object OptionsParser {
  def getOptions(args: Array[String]): Config = {
    val builder = scopt.OParser.builder[Config]
    val parser = {
      import builder._
      OParser.sequence(
        programName("MusvitMain"),
        head("Musvit generator", "0.1"),
        help('h', "help").text("Prints this message"),

        opt[String]('r', "rom-contents")
          .required()
          .text("Binary file of contents to put in boot ROM of Musvit")
          .valueName("<file>")
          .action((value, config) => config.copy(musvitConfig = config.musvitConfig.copy(romContents = value))),

        opt[Int]('c', "clock-frequency")
          .required()
          .text("Clock frequency of Musvit")
          .valueName("<value>")
          .action((value, config) => config.copy(musvitConfig = config.musvitConfig.copy(clockFrequency = value))),

        opt[Int]('w', "fetch-width")
          .text("Number of instruction fetched every memory access")
          .valueName("<value>")
          .action((value, config) => config.copy(musvitConfig = config.musvitConfig.copy(fetchWidth = value))),

        opt[String]('f', "firrtl-opts")
          .optional()
          .text("String of options to pass to firrtl")
          .valueName("<\"option options ... \">")
          .action((value, config) => config.copy(firrtlOpts = value.split(" "))),

        opt[String]('F', "firtool-opts")
          .optional()
          .text("String of options to pass to firtool")
          .valueName("<\"option options ... \">")
          .action((value, config) => config.copy(firtoolOpts = value.split(" "))),

        checkConfig(config => {
          if (!Files.exists(Paths.get(config.musvitConfig.romContents)) || Files.isDirectory(Paths.get(config.musvitConfig.romContents))) {
            failure("ERROR: " + config.musvitConfig.romContents + " is not a file")
          }
          success
        })
      )
    }

    OParser.parse(parser, args, Config()) match {
      case Some(config) =>
        config
      case _ =>
        exit(1)
    }
  }

  def printOptions(args: Array[String]): Unit = {
    val options = getOptions(args)
    print("ROM contents:    " + options.musvitConfig.romContents + "\n")
    print("FIRRTL options:  ")
    options.firrtlOpts.map(opt => print(opt + " "))
    print("\n")
    print("FIRTOOL options: ")
    options.firtoolOpts.map(opt => print(opt + " "))
    print("\n")
  }
}
