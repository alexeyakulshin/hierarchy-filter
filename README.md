# hierarchy-filter

Take-home for Tempo, two parts:

1. `Hierarchy.filter()` + tests, in `src/`
2. Review of `SimpleCache`, in [REVIEW.md](REVIEW.md)

Original assignment files are in `docs/`.

## Running

```bash
./gradlew test
```

You need a JDK installed to run Gradle, that's it. The build targets Java 21 and downloads that toolchain
automatically if you don't have it. Kotlin and Gradle come via the wrapper. Checked with Gradle 9.6 /
Kotlin 2.4.20 on JDK 25.

## How filter works

One pass over the DFS array. I keep a single int, `prunedDepth`: when a node fails the predicate at depth D,
everything after it with depth > D is its descendant and gets skipped without calling the predicate. The first
node with depth <= D is outside that subtree, so pruning stops and that node is checked normally.

Surviving nodes keep their original depths: if a node is included, all its ancestors are included too, so
there is nothing to recompute. I just copy them.

O(n) time, O(n) for the result (two `IntArray` buffers trimmed at the end). No recursion, no stack, no boxing.
Predicate is called at most once per node and never for descendants of a rejected node, there's a test for that.

## Layout

Single Gradle module, package `hierarchy`. It's one pure function over an immutable value, didn't see a reason
for more structure than that.

```
src/main/kotlin/hierarchy/
  Hierarchy.kt            interface, KDoc from the assignment
  ArrayBasedHierarchy.kt  array-backed impl
  HierarchyFilter.kt      filter()
src/test/kotlin/hierarchy/
  FilterTest.kt           the test from the assignment, untouched
  HierarchyFilterTest.kt  mine
```

Kept the names from the assignment (`Hierarchy`, `ArrayBasedHierarchy`, `filter`, `formatString`) so the original
test compiles as is.

## Assumptions

- Input respects the invariants from the `Hierarchy` doc (first depth is 0, depth grows by at most 1). I only
  check that the two arrays have the same length, full validation felt out of scope.
- Node ids are unique, as the interface says.
- Predicate is cheap and side-effect free. If it isn't (hits a DB etc.), precompute a set of allowed ids and
  filter by that.
- Input is never mutated, filter always returns a new hierarchy.

## If it ever needs to be faster

It's already linear and that's the lower bound in general. If the same hierarchy is filtered many times with
heavy pruning, one could precompute "index of the end of each subtree" once (O(n), one extra IntArray) and
skip rejected subtrees in O(1) instead of scanning through them. Didn't do it here, nothing in the task asks
for it.
