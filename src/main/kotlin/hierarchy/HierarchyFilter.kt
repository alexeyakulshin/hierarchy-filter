package hierarchy

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate and all of its ancestors pass it as well.
 *
 * Single pass, O(n). Depths are copied as-is: a kept node keeps all its ancestors, so its depth doesn't change.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
    val ids = IntArray(size)
    val depths = IntArray(size)
    var count = 0

    // depth of the last rejected node; everything deeper than it is its subtree and gets skipped.
    // MAX_VALUE = not skipping anything
    var prunedDepth = Int.MAX_VALUE

    for (i in 0 until size) {
        val depth = depth(i)
        if (depth > prunedDepth) continue

        val id = nodeId(i)
        if (nodeIdPredicate(id)) {
            prunedDepth = Int.MAX_VALUE
            ids[count] = id
            depths[count] = depth
            count++
        } else {
            prunedDepth = depth
        }
    }

    return ArrayBasedHierarchy(ids.copyOf(count), depths.copyOf(count))
}
