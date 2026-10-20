package hierarchy

/**
 * [Hierarchy] backed by two parallel primitive arrays.
 * The arrays are assumed to satisfy the invariants documented on [Hierarchy]; only their lengths are checked.
 */
class ArrayBasedHierarchy(
    private val myNodeIds: IntArray,
    private val myDepths: IntArray,
) : Hierarchy {
    init {
        require(myNodeIds.size == myDepths.size) {
            "nodeIds and depths must have the same length: ${myNodeIds.size} != ${myDepths.size}"
        }
    }

    override val size: Int = myDepths.size

    override fun nodeId(index: Int): Int = myNodeIds[index]

    override fun depth(index: Int): Int = myDepths[index]
}
