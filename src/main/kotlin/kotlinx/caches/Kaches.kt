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
        Eviction.LRU -> throw NotImplementedError()
        Eviction.LIFE_TIME -> LifeTimeEvictionCache(config)
        Eviction.IDLE_TIME -> IdleTimeEvictionCache(config)
    }
}

interface Cache<in K, out V> {

    fun get(key: K): V?

    fun contains(key: K): Boolean

    fun invalidate(key: K): V?

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

    var size = Int.MAX_VALUE

    var eviction = Eviction.RANDOM

    var lifeTime = Long.MAX_VALUE

    var idleTime = Long.MAX_VALUE

    var getValue: (K) -> V? = { throw IllegalArgumentException("Cached value lambda should be set") }

    var evictListener: ((K, V?) -> Unit)? = null

    var currentTimeMillis: () -> Long = { throw IllegalArgumentException("Current time lambda should be set"); }
}

private open class ValueEntry<out V>(val value: V?) {

    open fun isObsolete(): Boolean = false

    open fun touch() {}
}

private const val MIN_CACHE_SIZE = 2

private sealed class CacheBase<K, V>(protected val config: CacheConfig<K, V>) : Cache<K, V> {

    protected val map = hashMapOf<K, ValueEntry<V>>()

    init {
        if (config.size < MIN_CACHE_SIZE) {
            throw IllegalArgumentException("Cache size cannot be less than $MIN_CACHE_SIZE")
        }
    }

    override fun get(key: K): V? {
        val entry = map[key]
        if (entry != null) {
            if (!entry.isObsolete()) {
                entry.touch()
                return entry.value
            }
            invalidate(key)
        }

        while (count() >= size()) {
            invalidate(keyToEvict())
        }

        val value = config.getValue(key)
        map[key] = newValueEntry(key, value)
        return value
    }

    override fun contains(key: K): Boolean {
        val entry = map[key]
        if (entry != null) {
            if (!entry.isObsolete()) {
                return true
            }
            invalidate(key)
        }
        return false
    }

    override fun invalidate(key: K): V? {
        val removedEntry = map.remove(key)
        return removedEntry?.apply { evict(key, this) }?.value
    }

    override fun size() = config.size

    override fun count() = map.size

    protected open fun evict(key: K, valueEntry: ValueEntry<V>) {
        config.evictListener?.invoke(key, valueEntry.value)
    }

    protected abstract fun newValueEntry(key: K, value: V?): ValueEntry<V>

    protected abstract fun keyToEvict(): K

    companion object {

        fun requiredSizeLimit(size: Int, exceptionMsg: () -> String) {
            if (size == Int.MAX_VALUE) {
                throw IllegalArgumentException(exceptionMsg())
            }
        }

        fun requiredTimeLimit(time: Long, exceptionMsg: () -> String) {
            if (time == Long.MAX_VALUE) {
                throw IllegalArgumentException(exceptionMsg())
            }
        }
    }
}

private class RandomEvictionCache<K, V>(config: CacheConfig<K, V>) : CacheBase<K, V>(config) {

    private val entries: MutableMap<ValueEntry<V>, K> = hashMapOf()

    init {
        requiredSizeLimit(config.size, { "Size should be limited for random eviction cache" })
    }

    override fun newValueEntry(key: K, value: V?): ValueEntry<V> {
        return ValueEntry(value).apply { entries[this] = key }
    }

    override fun keyToEvict(): K {
        return entries.values.first()
    }

    override fun evict(key: K, valueEntry: ValueEntry<V>) {
        entries.remove(valueEntry)
        super.evict(key, valueEntry)
    }
}

private class LifeTimeValueEntry<K, out V>(private val config: CacheConfig<K, V>, value: V?) : ValueEntry<V>(value) {

    private val creationTime = config.currentTimeMillis()

    override fun isObsolete() = config.currentTimeMillis() - creationTime > config.lifeTime
}

private class LifeTimeEvictionCache<K, V>(config: CacheConfig<K, V>) : CacheBase<K, V>(config) {

    init {
        requiredTimeLimit(config.lifeTime, { "Life time should be limited for life-time eviction cache" })
    }

    override fun newValueEntry(key: K, value: V?): ValueEntry<V> {
        return LifeTimeValueEntry(config, value)
    }

    override fun keyToEvict(): K {
        throw IllegalStateException("Cache with life time eviction has unlimited size")
    }
}

private class IdleTimeValueEntry<K, out V>(private val config: CacheConfig<K, V>, value: V?) : ValueEntry<V>(value) {

    private var lastUsedTime = config.currentTimeMillis()

    override fun isObsolete() = config.currentTimeMillis() - lastUsedTime > config.idleTime

    override fun touch() {
        lastUsedTime = config.currentTimeMillis()
    }
}

private class IdleTimeEvictionCache<K, V>(config: CacheConfig<K, V>) : CacheBase<K, V>(config) {

    init {
        requiredTimeLimit(config.lifeTime, { "Life time should be limited for life-time eviction cache" })
    }

    override fun newValueEntry(key: K, value: V?): ValueEntry<V> {
        return IdleTimeValueEntry(config, value)
    }

    override fun keyToEvict(): K {
        throw IllegalStateException("Cache with life time eviction has unlimited size")
    }
}