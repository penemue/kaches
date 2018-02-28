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

private const val MIN_CACHE_SIZE = 2

internal open class ValueEntry<out V>(val value: V?) {

    open fun isObsolete(): Boolean = false

    open fun touch() {}
}

internal abstract class CacheBase<K, V>(protected val config: CacheConfig<K, V>) : Cache<K, V> {

    private val map = hashMapOf<K, ValueEntry<V>>()

    init {
        if (config.size < MIN_CACHE_SIZE) {
            throw IllegalArgumentException("Cache size cannot be less than $MIN_CACHE_SIZE")
        }
    }

    suspend override fun get(key: K): V? {
        val entry = map[key]
        if (entry != null) {
            if (!entry.isObsolete()) {
                touch(entry)
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

    suspend override fun contains(key: K): Boolean {
        val entry = map[key]
        if (entry != null) {
            if (!entry.isObsolete()) {
                return true
            }
            invalidate(key)
        }
        return false
    }

    suspend override fun invalidate(key: K): V? {
        val removedEntry = map.remove(key)
        return removedEntry?.apply { evict(key, this) }?.value
    }

    override fun size() = config.size

    override fun count() = map.size

    protected open fun touch(entry: ValueEntry<V>) = entry.touch()

    protected suspend open fun evict(key: K, entry: ValueEntry<V>) {
        config.evictListener?.invoke(key, entry.value)
    }

    protected abstract fun newValueEntry(key: K, value: V?): ValueEntry<V>

    protected abstract fun keyToEvict(): K

    companion object {

        fun requiredSizeLimit(size: Int, exceptionMsg: () -> String) {
            if (size == CacheConfig.NOT_SET_INT) {
                throw IllegalArgumentException(exceptionMsg())
            }
        }

        fun requiredTimeLimit(time: Long, exceptionMsg: () -> String) {
            if (time == CacheConfig.NOT_SET_LONG) {
                throw IllegalArgumentException(exceptionMsg())
            }
        }
    }
}