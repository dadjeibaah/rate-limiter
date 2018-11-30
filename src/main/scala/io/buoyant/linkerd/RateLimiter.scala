package io.buoyant.linkerd

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Duration, Future, Timer}

class RateLimiter(
  limit: Int,
  timer: Timer,
  window: Int
) extends SimpleFilter[Request, Response] {

  private[this] val count = new AtomicInteger()
  private[this] val decrementToZero: IntUnaryOperator = i => if (i > 0) i - 1 else i
  private[this] val period = window.toFloat / limit

  timer.schedule(Duration.fromFractionalSeconds(period))(count.getAndUpdate(decrementToZero))

  override def apply(
    request: Request,
    service: Service[Request, Response]
  ): Future[Response] = {
    if (count.get() >= limit) Future.value(Response(Status.TooManyRequests))
    else {
      count.getAndIncrement()
      service(request)
    }
  }
}
