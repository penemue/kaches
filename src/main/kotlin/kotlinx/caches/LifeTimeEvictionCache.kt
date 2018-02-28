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

private class LifeTimeValueEntry<K, out V>(private val config: CacheConfig<K, V>, value: V?) : ValueEntry<V>(value) {

    private val creationTime = config.currentTimeMillis()

    override fun isObsolete() = config.currentTimeMillis() - creationTime > config.lifeTime
}

internal class LifeTimeEvictionCache<K, V>(config: CacheConfig<K, V>) : CacheBase<K, V>(config) {

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