def twitterUtil(mod: String) =
  "com.twitter" %% s"util-$mod" %  "18.10.0"

def finagle(mod: String) =
  "com.twitter" %% s"finagle-$mod" % "18.10.0"

def linkerd(mod: String) =
  "io.buoyant" %% s"linkerd-$mod" % "1.5.1"

val rateLimiter = project.in(file("."))
  .settings(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    libraryDependencies ++=
      "org.scalatest" %% "scalatest" % "3.0.5" % "test" ::
      finagle("http") % "provided" ::
      twitterUtil("core") % "provided" ::
      linkerd("core") % "provided" ::
      linkerd("protocol-http") % "provided" ::
      Nil,
    name := "rate-limiter",
    organization := "io.buoyant",
    resolvers += "twitter" at "https://maven.twttr.com",
    scalaVersion := "2.12.1"
  )
