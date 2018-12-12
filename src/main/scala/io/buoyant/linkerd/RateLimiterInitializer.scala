package io.buoyant.linkerd

class RateLimiterInitializer extends RequestAuthorizerInitializer {
  val configClass: Class[_] = classOf[RateLimiterConfig]
  override val configId: String = "io.l5d.rateLimiter"
}

object RateLimiterInitializer extends RateLimiterInitializer
