# SimpleCache review

The expected load is thousands of reads and hundreds of writes per second, with tens of threads.

The concurrency approach looks reasonable, but I would block this before production. The main issue is that
expired entries remain in the map and there is no size limit, so on a long-running service this is a memory leak.

## Expired entries are never removed

`get()` returns null for an expired entry but leaves it in the map. If callers keep adding new keys, the cache
keeps growing even after those entries expire. In practice that looks like heap climbing slowly for days, GC
pauses getting longer, and eventually an `OutOfMemoryError`, usually long after anyone remembers this class
was touched.

Removing expired entries on read would help:

```kotlin
cache.remove(key, entry)
```

The two-argument overload matters here: it avoids deleting a newer entry written by another thread between the
read and the removal.

This still leaves entries that are never read again, so some background cleanup is needed. A size limit is also
necessary to bound memory usage when many distinct keys arrive within one TTL window. Those are two different
protections, and a production cache needs both.

For production, I would use Caffeine with `maximumSize` and `expireAfterWrite` rather than implement eviction
and cleanup here.

## size() includes expired entries

`size()` reports the number of stored entries, including those that `get()` would treat as expired. Anyone using
it for a metric or a "cache full" check gets a number that only goes up. It should either count live entries or
not exist; a misleading number is worse than none.

Even with Caffeine, `estimatedSize()` can include expired entries awaiting cleanup, so switching libraries does
not make this an exact count of live entries. At least it is documented as an estimate.

## Use a monotonic clock for TTL

`System.currentTimeMillis()` measures wall-clock time, which can change while the application is running. A
backward adjustment can keep entries alive longer than intended; a forward adjustment can expire them early, all
at once.

TTL should use elapsed time from a monotonic source, such as `System.nanoTime()` or Kotlin's
`TimeSource.Monotonic`. Replacing `currentTimeMillis()` with `Clock.systemUTC()` would not help, that is still
wall-clock time.

I would also inject the time source so expiration tests can advance time explicitly instead of sleeping.

## Concurrent misses can trigger duplicate loads

If callers load values on a cache miss, they will likely write something like:

```kotlin
cache.get(key) ?: load(key).also { cache.put(key, it) }
```

When a frequently accessed key expires, several threads can miss and load the same value concurrently. Whether
this needs fixing depends on how the cache is populated and how expensive loading is. If callers load on demand,
the cache should provide an operation that coordinates concurrent loads for the same key.

`ConcurrentHashMap.compute` could support a basic version that checks expiration and replaces the entry
atomically. `computeIfAbsent` alone would not work because expired entries are still present. However, running
a slow loader inside `compute` blocks other updates to that bucket, which is one more reason to prefer a library
with loading support.

## Invalidation is missing

There is no `remove()` or `clear()`. Callers can replace a value with `put()`, but they cannot invalidate it
without already having a replacement.

If serving an old value for up to a minute is acceptable, this may be sufficient. Otherwise, the cache needs an
explicit invalidation operation.

## Other improvements

- Make TTL a constructor parameter, preferably a `Duration`.
- Use `K : Any` and `V : Any` if null keys and values are unsupported. `ConcurrentHashMap` rejects null keys,
  while a null value inside `CacheEntry` would be indistinguishable from a miss through the current `get()` API.
- Make `CacheEntry` private; callers do not need access to the storage representation.
- Consider hit and miss metrics to check whether the cache is effective.

## What looks good

`ConcurrentHashMap` fits the expected access pattern. Entries are immutable and safely published through the map,
so reading their values and timestamps does not require additional locking. The concurrency is not what needs
fixing here.
