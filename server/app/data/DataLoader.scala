package data

import com.datastax.driver.core.Session
import dal.{CassandraCluster, CassandraConfig}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Logger, Play}

import scala.util.{Failure, Success, Try}

object DataLoader extends App {
  val blockSeparator = "\n/"
  val scriptsPath = "cassandra"

  lazy val app = new GuiceApplicationBuilder().build()

  lazy val cassandraConfig = app.injector.instanceOf[CassandraConfig]

  try {
    getCluster match {
      case Success(cluster) =>
        val session = cluster.noKeySpaceSession
        try {
          Logger.info("CQL scripts import start...")
          if (dropOptionEnabled(args)) dropKeySpace(cassandraConfig.keySpace, session)
          executeScripts(block => session.execute(block))
          Logger.info("CQL scripts import completed")
        } finally {
          session.close()
          cluster.stop()
        }
      case Failure(e) => Logger.error(s"cassandra instance error: ${e.getMessage}")
    }
  } finally Play.stop(app)

  private def dropOptionEnabled(args: Array[String]) = args.headOption.contains("drop")

  private def dropKeySpace(keySpace: String, session: Session) =
    Try(session.execute(s"drop schema $keySpace")) match {
      case Success(_) => Logger.info("key space has been dropped")
      case Failure(e) => Logger.warn(s"drop of key space has been failed, error: ${e.getMessage}")
    }

  private def getCluster = Try(app.injector.instanceOf[CassandraCluster])

  private def executeScripts(executor: String => Any) = {
    app.path.listFiles().filter(_.getName == scriptsPath).foreach(_.listFiles().sorted.foreach { f =>
      Logger.info(s"source file: ${f.getAbsolutePath}")

      val source = scala.io.Source.fromFile(f.getAbsolutePath)
      val blocks = try {
        source.mkString.split(blockSeparator).map(_.trim).filterNot(_.isEmpty)
      } finally source.close()

      blocks.foreach { b =>
        Logger.info(s"Running CQL:\n $b")
        executor(b)
      }
    })
  }
}
