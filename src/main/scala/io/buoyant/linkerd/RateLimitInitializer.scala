package io.buoyant.linkerd

import com.fasterxml.jackson.annotation.JsonIgnore
import com.twitter.finagle._
import com.twitter.util.Timer
import io.buoyant.linkerd.protocol.HttpRequestAuthorizerConfig


case class RateLimitConfig(
  limit: Int,
  window: Int,
  timer: Timer
)
  extends HttpRequestAuthorizerConfig {

  @JsonIgnore
  override def role: Stack.Role = Stack.Role("HTTP Rate Limiter")

  @JsonIgnore
  override def description: String = "Rate Limiter for HTTP"

  @JsonIgnore
  override def parameters: Seq[Stack.Param[_]] = Seq()

  @JsonIgnore
  override def mk(params: Stack.Params) = new RateLimitFilter(
    limit,
    window,
    timer
  )
}

class RateLimitInitializer extends RequestAuthorizerInitializer {
  val configClass = classOf[RateLimitConfig]
  override val configId = "io.l5d.rateLimit"
}

object RateLimitInitializer extends RateLimitInitializer
