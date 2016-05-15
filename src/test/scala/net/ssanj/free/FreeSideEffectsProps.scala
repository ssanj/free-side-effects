package net.ssanj.free

import org.scalacheck.Properties
import org.scalacheck.{Prop, Gen}

import net.ssanj.free.FreeSideEffects._
import net.ssanj.free.FreeSideEffects.SideEffect._

final class FreeSideEffectsProps extends Properties("FreeSideEffects") {

  private def wordGen: Gen[String] = for {
    l    <- Gen.choose(5, 10)
    word <- Gen.listOfN(l, Gen.alphaLowerChar)
  } yield word.mkString

  private def pathGen: Gen[String] = for {
    l     <- Gen.choose(5, 10)
    paths <- Gen.listOfN(l, wordGen)
  } yield paths.mkString("/")

  private def textFilenameGen: Gen[String] = for {
    name <- wordGen
    path <- pathGen
  } yield s"${path}/${name}.txt"

  private def contentGen: Gen[String] = for {
    l     <- Gen.choose(10, 20)
    words <- Gen.listOfN(l, wordGen)
  } yield words.mkString(" ")

  private def textFilenameAndContentGen: Gen[(String, String)] = for {
    name    <- textFilenameGen
    content <- contentGen
  } yield (name, content)

  private def testIntepreter(se: Script[Unit], world: Map[String, String]): Map[String, String] = se.resume.fold[Map[String, String]]({
    case DoesFileExist(filename, n) => testIntepreter(n(world.contains(filename)), world)
    case ReadFile(filename, n) => testIntepreter(n(world(filename)), world)
    case WriteFile(filename, content, n) => testIntepreter(n, world + (filename -> content))

  }, _ => world)

  property("should create a missing file") =
    Prop.forAllNoShrink(textFilenameAndContentGen) {
      case (filename: String, content: String) =>
        val world    = Map.empty[String, String]
        val output   = testIntepreter(program(filename, content), world)
        val expected = Map(filename -> content)
        output == expected
    }

  property("should updated an existing file") =
    Prop.forAllNoShrink(textFilenameAndContentGen) {
      case (filename: String, content: String) =>
        val world    = Map(filename -> content)
        val output   = testIntepreter(program(filename, content), world)
        val expected = Map(filename -> (content + "!!!"))
        output == expected
    }
}