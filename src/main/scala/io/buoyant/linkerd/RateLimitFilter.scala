package io.buoyant.linkerd

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Duration, Future, Time}
import scala.collection.mutable

class RateLimitFilter(
  limit: Int,
  window: Int
) extends SimpleFilter[Request, Response] {

  private[this] val RequestRateCounter = mutable.SortedMap[Time, Int]()

  override def apply(
    request: Request,
    service: Service[Request, Response]
  ): Future[Response] = {
    val now = Time.now

    val validReqs = RequestRateCounter.collect {
      case (t, c) if t > now.minus(Duration.fromSeconds(window)) => c
    }

    if (validReqs.sum >= limit)
      Future.value(Response(Status.TooManyRequests))
    else {
     RequestRateCounter.get(now) match {
       case Some(c) => RequestRateCounter.update(now, c+1)
       case None => RequestRateCounter.update(now, 1)
     }
      service(request)
    }
  }
}
