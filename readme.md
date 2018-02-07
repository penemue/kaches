# Kaches

[![Apache License 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Pure Kotlin](https://img.shields.io/badge/100%25-kotlin-orange.svg)](https://kotlinlang.org)

Generic caches written in [Kotlin](https://kotlinlang.org).

Create cache with `LRU` eviction having maximum `1000` items:
```Kotlin
    val cache = cache<Int, String> {
        eviction = Eviction.RANDOM
        size = 1000
        getValue = { key -> key.toString() }
    }
```

Create cache with `LIFE_TIME` eviction with cached values living one second:
```Kotlin
    val cache = cache<Int, String> {
        eviction = Eviction.LIFE_TIME
        lifeTime = 1000
        getValue = { key -> key.toString() }
    }
```

Create cache with `IDLE_TIME` eviction with cached values evicted after being unused for one second:
```Kotlin
    val cache = cache<Int, String> {
        eviction = Eviction.IDLE_TIME
        idleTime = 1000
        getValue = { key -> key.toString() }
    }
```
