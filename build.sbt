
def twitterUtil(mod: String) =
  "com.twitter" %% s"util-$mod" %  "18.10.0"

def finagle(mod: String) =
  "com.twitter" %% s"finagle-$mod" % "18.10.0"

def linkerd(mod: String) =
  "io.buoyant" %% s"linkerd-$mod" % "1.5.1"

val rateLimiter =
  project.in(file(".")).
    settings(
      scalaVersion := "2.12.1",
      organization := "io.buoyant",
      name := "rate-limiter",
      resolvers ++= Seq(
        "twitter" at "https://maven.twttr.com",
        "local-m2" at ("file:" + Path.userHome.absolutePath + "/.m2/repository")
      ),
      libraryDependencies ++=
        "org.scalatest" %% "scalatest" % "3.0.5" % "test" ::
        finagle("http") % "provided" ::
        twitterUtil("core") % "provided" ::
        linkerd("core") % "provided" ::
        linkerd("protocol-http") % "provided" ::
        Nil,
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
    )
