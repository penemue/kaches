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

/**
 * Minimalistic LinkedList implementation for LRU eviction caches.
 */
internal class LinkedList {

    var first: Node? = null
    var last: Node? = null
    var size = 0

    fun add(node: Node) {
        first = node
        if (last == null) {
            last = node
        }
        ++size
    }

    fun moveToFirst(node: Node) {
        if (first != node) {
            unlinkNode(node)
            node.prev = null
            node.next = first
            first = node
        }
    }

    fun remove(node: Node) {
        unlinkNode(node)
        --size
    }

    fun removeLast(): Node {
        return (last ?: throw IllegalStateException("The list is empty")).apply { remove(this) }
    }

    private fun unlinkNode(node: Node) {
        val prev = node.prev
        val next = node.next
        if (prev == null) {
            first = next
        } else {
            prev.next = next
        }
        if (next == null) {
            last = prev
        } else {
            next.prev = prev
        }
    }

    internal interface Node {

        var prev: Node?

        var next: Node?
    }
}