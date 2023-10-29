package utility

import chisel3._
import chisel3.util._

class MultiOutputArbiterIO[T <: Data](private val gen: T, val inputs: Int, val outputs: Int) extends Bundle {
  val in = Flipped(Vec(inputs, Decoupled(gen)))
  val out = Vec(outputs, Decoupled(gen))
}

class MultiOutputArbiter[T <: Data](private val gen: T, val inputs: Int, val outputs: Int) extends Module {
  val io = IO(new MultiOutputArbiterIO(gen, inputs, outputs))
  
  val arbs = Seq.tabulate(outputs)( (i) => Module(new Arbiter(gen, inputs - i)))
  arbs(0).io.in <> io.in
  io.out(0) <> arbs(0).io.out
  
  for (i <- 1 until outputs) {
    arbs(i).io.in <> VecInit(io.in.drop(i))
    val chosenOH = scanRightOr(UIntToOH(arbs(i - 1).io.chosen)).unary_~.asBools.drop(1)

    for (j <- 0 until arbs(i).io.in.length) {
      arbs(i).io.in(j).valid := chosenOH(j) && io.in.drop(i)(j).valid
    }
    io.out(i) <> arbs(i).io.out
  }
}
