package com.github.germanosin.sbt.less4j

import sbt._
import com.typesafe.sbt.web._
import sbt.Keys._
import com.github.sommeri.less4j.core.{ThreadUnsafeLessCompiler, DefaultLessCompiler}
import xsbti.{Severity, Problem}
import com.typesafe.sbt.web.incremental._
import com.github.sommeri.less4j.{LessSource, LessCompiler, Less4jException}
import com.typesafe.sbt.web.incremental
import com.github.sommeri.less4j.core.problems.{CompilationError, GeneralProblem}
import com.github.germanosin.less4j.CustomFileSource
import sbt.Task
import com.typesafe.sbt.web.incremental.OpSuccess
import sbt.Configuration


object Import {

  object Less4jKeys {
    val less4j = TaskKey[Seq[File]]("less4j", "Invoke the less compiler")

    val compress = SettingKey[Boolean]("less-compress", "Compress output by removing some whitespaces.")
    val sourceMap = SettingKey[Boolean]("less-source-map", "Outputs a v3 sourcemap.")
    val sourceMapFileInline = SettingKey[Boolean]("less-source-map-file-inline", "Whether the source map should be embedded in the output file")
    val sourceMapLessInline = SettingKey[Boolean]("less-source-map-less-inline", "Whether to embed the less code in the source map")
    val sourceMapRootpath = SettingKey[String]("less-source-map-rootpath", "Adds this path onto the sourcemap filename and less file paths.")

    val fileInputHasher = TaskKey[OpInputHasher[File]]("less-task-file-input-hasher", "A function that hashes a given file.")
    val taskMessage = SettingKey[String]("less-task-message", "The message to output for a task")
    val less4jConfig = TaskKey[Less4jConfig]("less4j-options", "The less4j config object.")
  }

}


case class Less4jConfig(
                         val compress:Boolean,
                         val paths:List[File],
                         val sourceMap:Boolean,
                         val sourceMapFileInline:Boolean,
                         val sourceMapLessInline:Boolean)
{
  val config = new LessCompiler.Configuration()
  config.setCompressing(compress)
  config.getSourceMapConfiguration.setLinkSourceMap(sourceMap)
  config.getSourceMapConfiguration.setInline(sourceMapFileInline)
  config.getSourceMapConfiguration.setIncludeSourcesContent(sourceMapLessInline)
  def getConfig = config
}

object SbtLess4j extends AutoPlugin {

  override def requires = SbtWeb

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport.Less4jKeys._
  import scala.collection.JavaConversions._

  private type FileOpResultMappings = Map[File, OpResult]

  private def FileOpResultMappings(s: (File, OpResult)*): FileOpResultMappings = Map(s: _*)


  val less4jUnscopedSettings = Seq(
    includeFilter := GlobFilter("main.less"),
    less4jConfig := Less4jConfig(
      compress.value,
      (sourceDirectories.value ++ resourceDirectories.value ++ webModuleDirectories.value).toList,
      sourceMap.value,
      sourceMapFileInline.value,
      sourceMapLessInline.value
    )
  )

  def lessFilesTask(task: TaskKey[Seq[File]],
                config: Configuration): Def.Initialize[Task[Seq[File]]] = Def.task {

    val sources = ((Keys.sources in task in config).value ** ((includeFilter in task in config).value -- (excludeFilter in task in config).value)).get


    implicit val opInputHasher = (fileInputHasher in task in config).value

    val results: (Set[File], Seq[Problem]) = incremental.syncIncremental((streams in config).value.cacheDirectory / "run", sources) {
      modifiedSources: Seq[File] =>
        if (modifiedSources.size > 0) {
          streams.value.log.info(s"${(taskMessage in task in config).value} on ${
            modifiedSources.size
          } source(s)")

          val resultBatches: Seq[(FileOpResultMappings, Seq[Problem])] = modifiedSources.filter(f => f.exists()).filterNot(f => f.isDirectory).pair(relativeTo((sourceDirectories in task in config).value)) map {
            case (modifiedSource,modifiedPath)  => {
              val targetDir = (resourceManaged in task in config).value
              val lessConfig = (less4jConfig in task in config).value

              val replaceName = if (lessConfig.getConfig.isCompressing) ".css" else ".min.css"
              val targetCss = targetDir / modifiedPath.replaceAll("\\.less", replaceName)
              val targetMap = targetDir / modifiedPath.replaceAll("\\.less", ".css.map")
              val targetPath = targetCss.getParentFile

              if (!targetPath.exists) targetPath.mkdirs()

              compileSource(modifiedSource, targetCss, targetMap, lessConfig)
            }
          }


          resultBatches.foldLeft((FileOpResultMappings(), Seq[Problem]())) {
            (allCompletedResults, completedResult) =>
              val (prevOpResults, prevProblems) = allCompletedResults
              val (nextOpResults, nextProblems) = completedResult
              (prevOpResults ++ nextOpResults, prevProblems ++ nextProblems)
          }

        } else {
          (FileOpResultMappings(), Nil)
        }
    }

    val (filesWritten, problems) = results

    CompileProblems.report((reporter in task).value, problems)

    filesWritten.toSeq
  }


  def compileSource(source:File, targetCss:File, targetMap:File, lessConfig:Less4jConfig):(FileOpResultMappings,Seq[Problem]) = {
    val lessCompiler = new ThreadUnsafeLessCompiler()

    try {
      val lessSource = new CustomFileSource(source, lessConfig.paths)
      val hashCode = lessSource.hashCode()
      val compilationResult = lessCompiler.compile(lessSource,lessConfig.getConfig)
      val errors = compilationResult.getWarnings.toList.map(
        problem => {
          new LineBasedProblem(problem.getMessage, Severity.Warn, problem.getLine, problem.getCharacter, getFileLine(problem.getFile, problem.getLine), problem.getFile)
        }
      ).toSeq

      scala.tools.nsc.io.File(targetCss).writeAll(compilationResult.getCss)
      scala.tools.nsc.io.File(targetMap).writeAll(compilationResult.getSourceMap)

      val readenSources = lessSource.getImportedSources.map(
        importSource => {
          importSource.asInstanceOf[CustomFileSource].getInputFile.getCanonicalFile
        }
      ).toSet ++ Set(source)

      val writtenSources = Set(targetCss,targetMap)

      (FileOpResultMappings( source -> OpSuccess(readenSources, writtenSources)), errors)
    } catch  {
      case e:Less4jException => {
        val errors = e.getErrors.toList.map(
          problem => problem match {
            case p: GeneralProblem => new com.typesafe.sbt.web.GeneralProblem(problem.getMessage, if (problem.getFile != null) problem.getFile else source )
            case p: CompilationError => new LineBasedProblem(problem.getMessage, Severity.Error, problem.getLine, problem.getCharacter, getFileLine(getFileFromSource(problem.getSource), problem.getLine), getFileFromSource(problem.getSource))
          }
        ).toSeq
        (FileOpResultMappings( source -> OpFailure), errors)
      }
    }
  }

  def getFileFromSource(source:LessSource):File = {
    val customFileSource = source.asInstanceOf[CustomFileSource]
    if (customFileSource!=null)
      customFileSource.getInputFile.getCanonicalFile
    else
      null
  }


  def getFileLine(source:File, lineNum:Int):String = {
    val file = try Left(io.Source.fromFile(source)) catch {
      case exc : Throwable=> Right(exc.getMessage)
    }
    val line = lineNum-1;
    val eitherLine = (for(f <- file.left;
                           line <- f.getLines.toStream.drop(line).headOption.toLeft("too few lines").left) yield
            if (line == "") Right("line is empty") else Left(line)).joinLeft

    eitherLine match {
      case Left(lineString) => lineString
      case _ => ""
    }
  }


  override def projectSettings: Seq[Setting[_]] = Seq(
    compress := false,
    sourceMap := true,
    sourceMapFileInline := false,
    sourceMapLessInline := false
  ) ++ inTask(less4j)(
    inConfig(Assets)(less4jUnscopedSettings) ++
    inConfig(TestAssets)(less4jUnscopedSettings) ++
      Seq(
        moduleName := "less4j",
        taskMessage in Assets := "LESS4J compiling",
        taskMessage in TestAssets := "LESS4J test compiling",
        includeFilter in Assets := "*.less",
        excludeFilter in TestAssets := "_*.less"        
      )
  )  ++ addLessFilesTasks(less4j) ++ Seq(
    target in less4j := webTarget.value / less4j.key.label,
    fileInputHasher := OpInputHasher[File](f =>
      OpInputHash.hashString(f.getAbsolutePath)
    ),
    less4j in Assets := (less4j in Assets).dependsOn(webModules in Assets).value,
    less4j in TestAssets := (less4j in TestAssets).dependsOn(webModules in TestAssets).value
  )

  def  addLessFilesTasks(sourceFileTask: TaskKey[Seq[File]]) : Seq[Setting[_]] = {
    Seq(
      sourceFileTask in Assets := lessFilesTask(sourceFileTask, Assets).value,
      sourceFileTask in TestAssets := lessFilesTask(sourceFileTask, TestAssets).value,
      resourceManaged in sourceFileTask in Assets := webTarget.value / sourceFileTask.key.label / "main",
      resourceManaged in sourceFileTask in TestAssets := webTarget.value / sourceFileTask.key.label / "test",
      sourceFileTask := (sourceFileTask in Assets).value
    )  ++
      inConfig(Assets)(addUnscopedLessSourceFileTasks(sourceFileTask)) ++
      inConfig(TestAssets)(addUnscopedLessSourceFileTasks(sourceFileTask))
  }

  private def addUnscopedLessSourceFileTasks(sourceFileTask: TaskKey[Seq[File]]): Seq[Setting[_]] = {
    Seq(
      resourceGenerators <+= sourceFileTask,
      managedResourceDirectories += (resourceManaged in sourceFileTask).value
    )
  }
}