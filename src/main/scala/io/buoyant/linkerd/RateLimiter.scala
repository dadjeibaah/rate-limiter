package io.buoyant.linkerd

import com.twitter.conversions.time._
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Duration, Future, Time}
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator

class RateLimiter(limit: Int, windowSecs: Duration) extends SimpleFilter[Request, Response] {
  private[this] val count = new AtomicInteger()
  private[this] var lastReset = Time.epoch

  override def apply(
    request: Request,
    service: Service[Request, Response]
  ): Future[Response] = {
    if (lastReset.untilNow >= windowSecs) {
      count.set(limit)
      lastReset = Time.now
    }
    
    if (count.getAndDecrement > 0) {
      service(request)
    } else {
      Future.value(Response(Status.TooManyRequests))
    }
  }
}
