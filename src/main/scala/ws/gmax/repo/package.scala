package ws.gmax

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

package object repo {
  abstract class Java2ScalaFuture(isAsync: Boolean) {

    implicit class RichListenableFuture[ResultSet](lf: ListenableFuture[ResultSet]) {

      def asScalaFuture: Future[ResultSet] = {
        if (isAsync) {
          val p = Promise[ResultSet]()

          Futures.addCallback(lf, new FutureCallback[ResultSet] {
            def onFailure(ex: Throwable): Unit = p failure ex

            def onSuccess(result: ResultSet): Unit = p success result
          })

          p.future
        } else {
          Future.successful(lf.get)
        }
      }
    }
  }
}
