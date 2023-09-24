package utility

import scopt.OParser
import scala.sys.exit
import java.io.File

case class Config(
  romContents:    File = new File(""),
  clockFrequency: Int = -1,
  firrtlOpts:     Array[String] = Array(),
  firtoolOpts:    Array[String] = Array())

object OptionsParser {
  def getOptions(args: Array[String]): Config = {
    val builder = scopt.OParser.builder[Config]
    val parser = {
      import builder._
      OParser.sequence(
        programName("MusvitMain"),
        head("Musvit generator", "0.1"),
        help('h', "help").text("Prints this message"),
        opt[File]('r', "rom-contents")
          .required()
          .text("Binary file of contents to put in boot ROM of Musvit")
          .valueName("<file>")
          .action((value, config) => config.copy(romContents = value)),
        opt[Int]('c', "clock-frequency")
          .required()
          .text("Clock frequency of Musvit")
          .valueName("<value>")
          .action((value, config) => config.copy(clockFrequency = value)),
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
          if (!config.romContents.exists() || config.romContents.isDirectory()) {
            failure("ERROR: " + config.romContents.toPath() + " is not a file")
          } else {
            success
          }
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
    print("ROM contents:    " + options.romContents.toString() + "\n")
    print("FIRRTL options:  ")
    options.firrtlOpts.map(opt => print(opt + " "))
    print("\n")
    print("FIRTOOL options: ")
    options.firtoolOpts.map(opt => print(opt + " "))
    print("\n")
  }
}
