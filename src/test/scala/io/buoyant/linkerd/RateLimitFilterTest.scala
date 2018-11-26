package io.buoyant.linkerd

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Await, Future, Time}
import org.scalatest.FunSuite
import com.twitter.conversions.time._

class RateLimitFilterTest extends FunSuite {

  def mkService(counter: Int) = Service.mk[Request, Response] { _ =>
    Future.value(Response(Status.Ok))
  }

  test("can initialize filter") {
    val limiter = new RateLimitFilter(1, 1)
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        Future.value(Response(Status.Ok))
      }
    }

    val rep = Await.result(svc(Request()))
    assert(rep.status == Status.Ok)
  }

  test("allow requests to reach service when below rate limit") {
    val limiter = new RateLimitFilter(100, 1)
    @volatile var totalallowedReqs = 0
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        totalallowedReqs = totalallowedReqs + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 to 10) {
        svc(Request())
      }
      tc.advance(1.second)
      assert(totalallowedReqs == 10)
    }
  }

  test("block requests from reaching service when above rate limit") {
    val limiter = new RateLimitFilter(10, 1)
    @volatile var totalallowedReqs = 0
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        totalallowedReqs = totalallowedReqs + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      for (_ <- 1 to 12) {
        svc(Request())
      }
      tc.advance(1.second)
      assert(totalallowedReqs == 10)
    }
  }

  test("block requests from reaching service over time") {
      val limiter = new RateLimitFilter(10, 1)
      @volatile var totalallowedReqs = 0
      val svc = limiter.andThen {
        Service.mk[Request, Response] { _ =>
          totalallowedReqs = totalallowedReqs + 1
          Future.value(Response(Status.Ok))
        }
      }

      Time.withCurrentTimeFrozen { tc =>
        for (_ <- 1 to 12) {
          svc(Request())
        }
        tc.advance(1.second)
        assert(totalallowedReqs == 10)

        for(_ <- 1 to 8) {
          svc(Request())
        }

        tc.advance(1.second)
        assert(totalallowedReqs == 18)

        for(_ <- 1 to 2) {
          svc(Request())
        }
        tc.advance(1.second)

        for(_ <- 1 to 11){
          svc(Request())
        }
        tc.advance(1.second)
        assert(totalallowedReqs == 30)
      }
  }

  test("don't allow rate limit to exceed during window boundary transition") {
    val limiter = new RateLimitFilter(10, 1)
    @volatile var totalallowedReqs = 0
    val svc = limiter.andThen {
      Service.mk[Request, Response] { _ =>
        totalallowedReqs = totalallowedReqs + 1
        Future.value(Response(Status.Ok))
      }
    }

    Time.withCurrentTimeFrozen { tc =>
      tc.advance(9999.millisecond)

      for(_ <- 1 to 10) {
        svc(Request())
      }

      tc.advance(1.millisecond)

      for(_ <- 1 to 10) {
        svc(Request())
      }
      tc.advance(1.millisecond)

      assert(totalallowedReqs == 10)
    }
  }
}
