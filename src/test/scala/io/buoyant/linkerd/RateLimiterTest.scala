package io.buoyant.linkerd

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util._
import org.scalatest.FunSuite

class RateLimiterTest extends FunSuite {
  val limit = 100
  val invalidLimit: Int = -1

  val intervalSeconds = 1
  val invalidIntervalSeconds: Int = -1

  val period: Float = intervalSeconds.toFloat / limit

  test("initialize filter config with invalid request limit and fail") {
    assertThrows[IllegalArgumentException] {
      val _ = new RateLimiterConfig(invalidLimit, intervalSeconds)
    }
  }

  test("initialize filter config with invalid interval and fail") {
    assertThrows[IllegalArgumentException] {
      val _ = new RateLimiterConfig(limit, invalidIntervalSeconds)
    }
  }

  test("initialize filter config with default interval") {
    val limiterConfig = new RateLimiterConfig(limit)
    assert(limiterConfig.isInstanceOf[RateLimiterConfig])
  }

  test("accept all requests while under limit") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, intervalSeconds)
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
    val limiter = new RateLimiter(limit, timer, intervalSeconds)
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
    val limiter = new RateLimiter(limit, timer, intervalSeconds)
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
    val limiter = new RateLimiter(limit, timer, intervalSeconds)
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

  test("accept all requests after reaching the limit and receiving one request per period for the full intervalSeconds") {
    @volatile var allowedRequests = 0

    val timer = new MockTimer
    val limiter = new RateLimiter(limit, timer, intervalSeconds)
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
