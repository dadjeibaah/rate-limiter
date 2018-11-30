package io.buoyant.linkerd

import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util._
import org.scalatest.FunSuite

class RateLimiterTest extends FunSuite {
  val limit = 100
  val window = 1
  val period = window.toFloat / limit

  test("initialize filter and accept one request") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, window)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        allowedRequests = allowedRequests + 1
        Future.value(Response(Status.Ok))
      }
    }

    svc(Request())
    assert(allowedRequests == 1)
  }

  test("accept all requests while under limit") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, window)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        allowedRequests = allowedRequests + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 until limit) {
        svc(Request())
      }
    }

    assert(allowedRequests == limit - 1)
  }

  test("accept all requests up to the limit") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, window)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        allowedRequests = allowedRequests + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 to limit) {
        svc(Request())
      }
    }

    assert(allowedRequests == limit)
  }

  test("reject requests after reaching the limit") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, window)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        allowedRequests = allowedRequests + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 to limit + 1) {
        svc(Request())
      }
    }

    assert(allowedRequests == limit)
  }

  test("accept one request after reaching the limit and advancing one period") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, window)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        allowedRequests = allowedRequests + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 to limit) {
        svc(Request())
      }

      tc.advance(Duration.fromFractionalSeconds(period))
      timer.tick()

      svc(Request())
    }

    assert(allowedRequests == limit + 1)
  }

  test("accept all requests after reaching the limit and receiving one request per period for the full window") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, window)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        allowedRequests = allowedRequests + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 to limit) {
        svc(Request())
      }

      for (_ <- 1 to limit) {
        tc.advance(Duration.fromFractionalSeconds(period))
        timer.tick()
        svc(Request())
      }
    }

    assert(allowedRequests == limit * 2)
  }
}
