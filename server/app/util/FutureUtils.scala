package util

import java.util.concurrent.Executor

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}


object FutureUtils {

  import com.google.common.util.concurrent.{ListenableFuture => GuavaFuture}

  def toFuture[T](f: GuavaFuture[T])(implicit executor: ExecutionContext): Future[T] = {
    val pr = Promise[T]()
    f.addListener(new Runnable {
      def run() = pr.complete(Try(f.get()))
    }, executor match {
      case e: Executor => e
      case _ => new ExecutionContextExecutor(executor)
    })

    pr.future
  }

  def toFutureUnit[T](f: GuavaFuture[T])(implicit executor: ExecutionContext): Future[Unit] = {
    val pr = Promise[Unit]()
    f.addListener(new Runnable {
      def run() = pr.complete(Try(f.get()))
    }, executor match {
      case e: Executor => e
      case _ => new ExecutionContextExecutor(executor)
    })

    pr.future
  }

  private class ExecutionContextExecutor(executonContext: ExecutionContext) extends java.util.concurrent.Executor {
    def execute(command: Runnable): Unit = executonContext.execute(command)
  }

}

object TryFuture {
  def apply[A](block: => Future[A]): Future[A] = Try(block) match {
    case Success(s) => s
    case Failure(f) => Future.failed(f)
  }

  implicit class RichTry[A](val wrapped: Try[A]) extends AnyVal {
    def toFuture = wrapped match {
      case Success(value) => Future.successful(value)
      case Failure(e) => Future.failed(e)
    }
  }

}
