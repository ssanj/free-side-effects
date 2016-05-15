package net.ssanj.free

import scalaz._
import Free._

/**
 * A simple Free Monad example based on KVS Example: https://git.io/vrGIs
 *
 * A Free Monad that can:
 * 1. Query the existence of a file.
 * 2. Read a file.
 * 3. Write a file.
 *
 * Has two interpreters:
 * 1. consoleInterpreter - writes what it's doing to the console
 * 2. testIntepreter - writes state to a map.
 * 3. FilesystemInterpreter - todo: will read/write from the file system
 */
object FreeSideEffects {

  //1. DSL
  sealed trait SideEffect[+A]
  final case class DoesFileExist[A](filename: String, next: Boolean => A) extends SideEffect[A]
  final case class ReadFile[A](filename: String, next: String => A) extends SideEffect[A]
  final case class WriteFile[A](filename: String, content: String, next: A) extends SideEffect[A]

  //2. Functor
  //
  object SideEffect {
    type Script[A] = Free[SideEffect, A]

    implicit val seFunctor: Functor[SideEffect] = new Functor[SideEffect] {
      def map[A, B](se: SideEffect[A])(f: A => B): SideEffect[B] = se match {
        case DoesFileExist(filename, n) => DoesFileExist(filename, n andThen f)
        case ReadFile(filename, n) => ReadFile(filename, n andThen f)
        case WriteFile(filename, content, n) => WriteFile(filename, content, f(n))
      }
    }
  }

  import SideEffect._
  //3. Lifting Functions
  def doesFileExist(filename: String): Script[Boolean] = liftF(DoesFileExist(filename, identity))

  def readFile(filename: String): Script[String] = liftF(ReadFile(filename, identity))

  def writeFile(filename: String, content: String): Script[Unit] = liftF(WriteFile(filename, content, ()))

  //4. Composite functions
  def readWrite(filename: String, f: String => String): Script[Unit] = for {
    content <- readFile(filename)
    newContent = f(content)
    _ <- writeFile(filename, newContent)
  } yield ()

  //5. Write scripts
  def program(filename: String, default: String): Script[Unit] = for {
    exists <- doesFileExist(filename)
    _ <- if (exists) readWrite(filename, _ + "!!!") else writeFile(filename, default)
  } yield ()

  //6. interpreters -- see FreeSideEffectsProps for a test interpreter
  def consoleInterpreter(se: Script[Unit], fileExists: Boolean): Unit = se.go {
    case DoesFileExist(filename, n) =>
      println(s"checking if $filename exists. Returning $fileExists")
      n(fileExists)

    case ReadFile(filename, n) =>
      val content = "this has been read"
      println(s"""reading contents of: $filename and returning: "${content}" """)
      n(content)

    case WriteFile(filename, content, n) =>
      println(s"writing out: $filename with content: $content")
      n
  }

  def main(args: Array[String]): Unit = {
    println("===== console ====")
    println("_file does not exist_")
    consoleInterpreter(program("some/file/somewhere/blurb.txt", "you aint got no content"), false)
    println("_file exists_")
    consoleInterpreter(program("some/file/somewhere/blurb.txt", "you aint got no content"), true)
  }
}

