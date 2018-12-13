package io.buoyant.linkerd

import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util._
import org.scalatest.FunSuite

class RateLimiterTest extends FunSuite {
  val limit = 100
  val windowSecs = 1
  val windowSecsDuration: Duration = windowSecs.seconds
  
  val period = windowSecsDuration / limit
  
  val invalidLimit: Int = -1
  val invalidWindowSecs: Int = -1

  test("initialize filter config with invalid request limit and fail") {
    assertThrows[IllegalArgumentException] {
      val _ = new RateLimiterConfig(invalidLimit, windowSecs)
    }
  }

  test("initialize filter config with invalid window and fail") {
    assertThrows[IllegalArgumentException] {
      val _ = new RateLimiterConfig(limit, invalidWindowSecs)
    }
  }

  test("initialize filter config with default window") {
    val limiterConfig = new RateLimiterConfig(limit)
    assert(limiterConfig.isInstanceOf[RateLimiterConfig])
  }

  test("accept all requests while under limit") {
    @volatile var allowedRequests = 0

    val limiter = new RateLimiter(limit, windowSecsDuration)
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

    val limiter = new RateLimiter(limit, windowSecsDuration)
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

    val limiter = new RateLimiter(limit, windowSecsDuration)
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

  test("accept one request after reaching the limit and advancing one window") {
    @volatile var allowedRequests = 0

    val limiter = new RateLimiter(limit, windowSecsDuration)
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

      tc.advance(windowSecsDuration)
      svc(Request())
    }

    assert(allowedRequests == limit + 1)
  }

  test("accept all requests at the start, and then reject every request until the end of the window. Then, start accepting again.") {
    @volatile var allowedRequests = 0

    val limiter = new RateLimiter(limit, windowSecsDuration)
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

      for (_ <- 1 until limit * 2) {
        tc.advance(period)
        svc(Request())
      }
    }

    assert(allowedRequests == limit * 2)
  }

  test("accept all requests at the start, and then advance two windows. Accept only the limit after that.") {
    @volatile var allowedRequests = 0

    val limiter = new RateLimiter(limit, windowSecsDuration)
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

      for (_ <- 1 to 2) {
        tc.advance(windowSecsDuration)
      }

      for (_ <- 1 to limit * 2) {
        svc(Request())
      }
    }

    assert(allowedRequests == limit * 2)
  }
}
