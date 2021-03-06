package service

import monifu.concurrent.Implicits.globalScheduler
import org.scalatest.{FlatSpec, Matchers, Suite}

import scala.util.Try

class ScalaTestRunnerTest extends FlatSpec with Matchers with ScalaTestCorrectSolution {
  behavior of "ScalaTestRunner"
  val incorrectSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {weekday || vacation}}"

  it should "return success when correct solution is provided" in {
    getReport(correctSolution).isSuccess shouldBe true
  }

  it should "return success when compilable solution is provided" in {
    getReport(incorrectSolution).isSuccess shouldBe true
  }

  it should "return failure when check compilable but wrong solution" in {
    getReport(incorrectSolution, check = true).isFailure shouldBe true
  }

  it should "return failure when solution is not compilable" in {
    getReport("/").isFailure shouldBe true
  }

  private val runner = new ScalaRuntimeRunner() {}

  def getReport(solution: String, check: Boolean = false) = {
    val unchecked = Try(StringBuilderRunner(runner(
      Class.forName("service.SleepInTest").asInstanceOf[Class[Suite]],
      Class.forName("service.SleepInSolution").asInstanceOf[Class[AnyRef]],
      solution)))
    if (check) unchecked.check
    else unchecked
  }
}

trait ScalaTestCorrectSolution {
  val correctSolution = "class A { def sleepIn(weekday: Boolean, vacation: Boolean): Boolean = {!weekday || vacation}}"
}
