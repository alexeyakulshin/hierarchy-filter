package hierarchy

import kotlin.test.Test
import kotlin.test.assertEquals

class HierarchyFilterTest {

    /** The forest from the [Hierarchy] documentation. */
    private val forest: Hierarchy = hierarchy(
        intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
        intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2),
    )

    @Test
    fun `empty hierarchy stays empty`() {
        val empty = hierarchy(intArrayOf(), intArrayOf())
        val filtered = empty.filter { error("predicate must not be called for an empty hierarchy") }
        assertEquals(0, filtered.size)
    }

    @Test
    fun `predicate accepting everything returns an equal hierarchy`() {
        assertEquals(forest.formatString(), forest.filter { true }.formatString())
    }

    @Test
    fun `predicate rejecting everything returns an empty hierarchy`() {
        assertEquals(0, forest.filter { false }.size)
    }

    @Test
    fun `rejected root drops its whole tree and the next tree survives`() {
        val filtered = forest.filter { it != 1 }
        assertEquals("[6:0, 7:1, 8:0, 9:1, 10:1, 11:2]", filtered.formatString())
    }

    @Test
    fun `rejected leaf drops only itself`() {
        // the node after 4 is two levels shallower, the "d < D" case from the invariant
        val filtered = forest.filter { it != 4 }
        assertEquals("[1:0, 2:1, 3:2, 5:1, 6:0, 7:1, 8:0, 9:1, 10:1, 11:2]", filtered.formatString())
    }

    @Test
    fun `rejected inner node drops its subtree but keeps the following sibling`() {
        // 3 and 4 pass the predicate but are descendants of 2.
        val filtered = forest.filter { it != 2 }
        assertEquals("[1:0, 5:1, 6:0, 7:1, 8:0, 9:1, 10:1, 11:2]", filtered.formatString())
    }

    @Test
    fun `two rejected siblings in a row each drop their own subtree`() {
        // 9 and 10 are siblings under 8; rejecting both must not let 11 (child of 10) through
        val filtered = forest.filter { it != 9 && it != 10 }
        assertEquals("[1:0, 2:1, 3:2, 4:3, 5:1, 6:0, 7:1, 8:0]", filtered.formatString())
    }

    @Test
    fun `predicate is called once per visited node and never for descendants of a rejected node`() {
        val seen = mutableListOf<Int>()
        forest.filter { id ->
            seen += id
            id != 2 && id != 8
        }
        // 3 and 4 are under 2; 9, 10 and 11 are under 8.
        assertEquals(listOf(1, 2, 5, 6, 7, 8), seen)
    }

    @Test
    fun `deep chain does not overflow the stack`() {
        val count = 100_000
        val chain = hierarchy(IntArray(count) { it }, IntArray(count) { it })
        val filtered = chain.filter { it < count - 1 }
        assertEquals(count - 1, filtered.size)
        assertEquals(count - 2, filtered.nodeId(filtered.size - 1))
        assertEquals(count - 2, filtered.depth(filtered.size - 1))
    }

    private fun hierarchy(ids: IntArray, depths: IntArray): Hierarchy = ArrayBasedHierarchy(ids, depths)
}
