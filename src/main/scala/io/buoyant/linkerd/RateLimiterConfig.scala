package io.buoyant.linkerd

import com.fasterxml.jackson.annotation.JsonIgnore
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.{Filter, Stack}
import io.buoyant.linkerd.protocol.HttpRequestAuthorizerConfig

class RateLimiterConfig(limit: Int, window: Int) extends HttpRequestAuthorizerConfig{
  @JsonIgnore
  override def role: Stack.Role = Stack.Role("HttpRateLimiter")

  @JsonIgnore
  override def description: String = "HTTP Rate Limiter"

  @JsonIgnore
  override def parameters: Seq[Stack.Param[_]] = Seq()

  @JsonIgnore
  override def mk(params: Stack.Params): Filter[Request, Response, Request, Response] = {
    new RateLimiter(limit, DefaultTimer, window)
  }
}