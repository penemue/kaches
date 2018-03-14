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

private class LRUValueEntry<out K, out V>(var queue: LinkedList,
                                          val key: K,
                                          value: V?) : ValueEntry<V>(value), LinkedList.Node {

    init {
        queue.add(this)
    }

    override var prev: LinkedList.Node? = null

    override var next: LinkedList.Node? = null
}

internal class LRUEvictionCache<K, V>(config: CacheConfig<K, V>) : CacheBase<K, V>(config) {

    private val halfSize: Int
    private val probationQueue = LinkedList()
    private val protectedQueue = LinkedList()

    init {
        requiredSizeLimit(config.size, { "Size should be limited for LRU eviction cache" })
        halfSize = config.size / 2
    }

    override fun newValueEntry(key: K, value: V?): ValueEntry<V> {
        return LRUValueEntry(probationQueue, key, value)
    }

    override fun keyToEvict(): K {
        adjustProtectedQueue()
        @Suppress("UNCHECKED_CAST")
        val entry = probationQueue.removeLast() as LRUValueEntry<K, *>
        return entry.key
    }

    override fun touch(entry: ValueEntry<V>) {
        val lruEntry = entry as LRUValueEntry<*, *>
        if (lruEntry.queue === protectedQueue) {
            // move the entry to the head of protected queue
            protectedQueue.moveToFirst(entry)
        } else {
            // if the entry is in probation queue, move it to protected queue
            probationQueue.remove(entry)
            addEntryToQueue(lruEntry, protectedQueue)
            adjustProtectedQueue()
        }
    }

    override suspend fun evict(key: K, entry: ValueEntry<V>) {
        val lruEntry = entry as LRUValueEntry<*, *>
        lruEntry.queue.remove(lruEntry)
        super.evict(key, entry)
    }

    private fun adjustProtectedQueue() {
        while (protectedQueue.size > halfSize) {
            val entry = protectedQueue.removeLast() as LRUValueEntry<*, *>
            addEntryToQueue(entry, probationQueue)
        }
    }

    companion object {

        private fun addEntryToQueue(entry: LRUValueEntry<*, *>, queue: LinkedList) {
            queue.add(entry)
            entry.queue = queue
        }
    }
}