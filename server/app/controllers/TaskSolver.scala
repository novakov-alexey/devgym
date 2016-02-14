package controllers

import java.util.UUID

import com.google.inject.Inject
import controllers.TaskSolver._
import dal.Dao
import dal.Dao.now
import models.TaskType
import models.TaskType.scalaClass
import monifu.concurrent.Scheduler
import org.apache.commons.lang3.StringUtils
import org.scalatest.Suite
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, WebSocket}
import service._
import shared.Line

import scala.sys.process._
import scala.util.Try

class TaskSolver @Inject()(executor: RuntimeSuiteExecutor, dao: Dao, val messagesApi: MessagesApi)
                          (implicit s: Scheduler) extends Controller with I18nSupport with JSONFormats {
  val appPath = current.path.getAbsolutePath

  val solutionForm = Form {
    mapping(
      solution -> nonEmptyAndDiffer(StringUtils.EMPTY)
    )(SolutionForm.apply)(SolutionForm.unapply)
  }

  def getTask(year: Long, taskType: String, timeuuid: UUID) = Action.async { implicit request =>
    val task = dao.getTask(year, TaskType.withName(taskType), timeuuid)
    task.map {
      case Some(t) => Ok(views.html.task(t.description, solutionForm.fill(SolutionForm(t.solutionTemplate))))
      case None => Redirect(routes.Application.index).flashing(flashToUser -> messagesApi("taskNotFound"))
    }
  }

  def taskStream = WebSocket.acceptWithActor[String, JsValue] { req => out =>
    SimpleWebSocketActor.props(out, (solution: String) =>
      ObservableRunner(executor(
        Class.forName("tasktest.SubArrayWithMaxSumTest").asInstanceOf[Class[Suite]],
        Class.forName("tasktest.SubArrayWithMaxSumSolution").asInstanceOf[Class[AnyRef]],
        solution)).map(Line(_)),
      Some(Line("Compiling...")))
  }
}

case class SolutionForm(solution: String)

object TaskSolver {
  val flashToUser = "flashToUser"
  val cannotCheckNow = "cannotCheckNow"
  val solution = "solution"

  def nonEmptyAndDiffer(from: String) = nonEmptyText verifying Constraint[String]("changes.required") { o =>
    if (o.filter(_ != '\r') == from) Invalid(ValidationError("error.changesRequired")) else Valid
  }

  def sbt(command: String): Try[Boolean] = Try(Seq("sbt", command).! == 0)

  lazy val sbtInstalled = sbt("sbtVersion").isSuccess // no exception, so sbt is in the PATH
}
