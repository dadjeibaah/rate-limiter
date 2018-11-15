package io.buoyant.linkerd

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.{Duration, Future, Stopwatch, Time}
import scala.collection.mutable

class RateLimitFilter[Req, Rep](
  limitExceededResponse: => Rep,
  limit: Int,
  window: Duration
) extends SimpleFilter[Req, Rep] {

  private[this] val RequestRateCounter = mutable.SortedMap[(Req, Time), Int]()

  override def apply(
    request: Req,
    service: Service[Req, Rep]
  ): Future[Rep] = {
    val now = Time.now

    val validReqs = RequestRateCounter.collect {
      case ((req, time), count) if time < now.minus(window) => count
    }

    if (validReqs.sum > limit)
      Future.value(limitExceededResponse)
    else {
     RequestRateCounter.get((request, now)) match {
       case Some(c) => RequestRateCounter.update((request, now), c+1)
       case None => RequestRateCounter.update((request, now), 1)
     }
    }
    service(request)

  }

}
