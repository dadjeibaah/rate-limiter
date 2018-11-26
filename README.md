# Rate Limiter
This is a rate limiter plugin for Linkerd. It gives you the option to apply
a rate limit to each **endpoint** associated with a client name. This is
meant to be a POC to investigate how we can add rate limiting as a plugin
for Linkerd. Comments, issues and PR's are welcome.

To build the plugin, run:
```bash
./sbt rate-limiter:assembly
```
