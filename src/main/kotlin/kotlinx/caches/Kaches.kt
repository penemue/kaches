/**
 * Copyright 2018 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.caches

fun <K, V> cache(configurator: CacheConfig<K, V>.() -> Unit): Cache<K, V> {
    val config = CacheConfig<K, V>().apply(configurator)
    return when (config.eviction) {
        Eviction.RANDOM -> RandomEvictionCache(config)
        Eviction.LRU -> LRUEvictionCache(config)
        Eviction.LIFE_TIME -> LifeTimeEvictionCache(config)
        Eviction.IDLE_TIME -> IdleTimeEvictionCache(config)
    }
}

interface Cache<in K, out V> {

    suspend fun get(key: K): V?

    suspend fun contains(key: K): Boolean

    suspend fun invalidate(key: K): V?

    fun size(): Int

    fun count(): Int
}

enum class Eviction {
    RANDOM,
    LRU,
    LIFE_TIME,
    IDLE_TIME
}

class CacheConfig<K, V> {

    var size = NOT_SET_INT

    var eviction = Eviction.RANDOM

    var lifeTime = NOT_SET_LONG

    var idleTime = NOT_SET_LONG

    var getValue: suspend (K) -> V? = { throw IllegalArgumentException("Cached value lambda should be set") }

    var evictListener: (suspend (K, V?) -> Unit)? = null

    var currentTimeMillis: () -> Long = { throw IllegalArgumentException("Current time lambda should be set"); }

    companion object {

        const val NOT_SET_INT = Int.MAX_VALUE
        const val NOT_SET_LONG = Long.MAX_VALUE
    }
}