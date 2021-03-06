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

internal class RandomEvictionCache<K, V>(config: CacheConfig<K, V>) : CacheBase<K, V>(config) {

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

    override suspend fun evict(key: K, entry: ValueEntry<V>) {
        entries.remove(entry)
        super.evict(key, entry)
    }
}