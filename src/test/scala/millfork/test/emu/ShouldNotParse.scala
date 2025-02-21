package millfork.test.emu

import fastparse.core.Parsed.{Failure, Success}
import millfork.compiler.LabelGenerator
import millfork.parser._
import millfork._
import org.scalatest.Matchers

/**
  * @author Karol Stasiak
  */
object ShouldNotParse extends Matchers {
  def apply(source: String): Unit = {
    checkCase(Cpu.Mos, source)
    checkCase(Cpu.Z80, source)
    checkCase(Cpu.Motorola6809, source)
  }

  private def checkCase(cpu: Cpu.Value, source: String) {
    Console.out.flush()
    Console.err.flush()
    val log = TestErrorReporting.log
    println(source)
    val platform = EmuPlatform.get(cpu)
    val options = CompilationOptions(platform, Map(CompilationFlag.LenientTextEncoding -> true), None, platform.zpRegisterSize, Map(), JobContext(log, new LabelGenerator))
    log.hasErrors = false
    log.verbosity = 999
    var effectiveSource = source
    log.setSource(Some(effectiveSource.linesIterator.toIndexedSeq))
    val PreprocessingResult(preprocessedSource, features, _) = Preprocessor.preprocessForTest(options, effectiveSource)
    val parserF =
      platform.cpuFamily match {
        case CpuFamily.M6502 =>
          MosParser("", preprocessedSource, "", options, features)
        case CpuFamily.M6809 =>
          M6809Parser("", preprocessedSource, "", options, features)
        case CpuFamily.I80 =>
          Z80Parser("", preprocessedSource, "", options, features, options.flag(CompilationFlag.UseIntelSyntaxForInput))
      }
    parserF.toAst match {
      case Success(program, _) =>
        fail("Parse succeded")
      case f: Failure[_, _] =>
        println(f.extra.toString)
        println(f.lastParser.toString)
        log.info("Expected syntax error: " + parserF.lastLabel, Some(parserF.lastPosition))
    }
  }
}
