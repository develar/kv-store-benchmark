package org.jetbrains;

import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.sqlite.LongBinder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

/**
 * “Get” test: Populate a map with a pre-generated set of keys (in the JMH setup), make ~50% successful and ~50% unsuccessful “get” calls.
 * For non-identity maps with object keys we use a distinct set of keys (the different object with the same value is used for successful “get” calls).

 * "Put/update" test: Add a pre-generated set of keys to the map.
 * In the second loop add the equal set of keys (different objects with the same values) to this map again (make the updates).
 * <p>
 * “Put/remove” test: In a loop: add 2 entries to a map, remove 1 of existing entries (“add” pointer is increased by 2 on each iteration, “remove” pointer is increased by 1).
 */
public class ObjectToObjectBenchmark  {
  @Benchmark
  public Object get_sqlite(BenchmarkSqliteGetState state, Blackhole blackhole) {
    int result = 0;
    ImageKey[] keys = state.keys;
    var connection = state.connection;
    // assume that we will have some pool of such statements for concurrent use
    var binder = new LongBinder(2, 1);
    // executing under one read transaction is noticeably faster
    connection.beginTransaction();
    try (var statement = connection.prepareStatement("select data from data where contentDigest = ? and contentLength = ?", binder)) {
      for (ImageKey key : keys) {
        binder.bind(key.contentDigest, key.contentLength);
        var blob = statement.selectByteArray();
        if (blob != null) {
          result ^= blob.length;
        }
      }
    }
    connection.commit();
    blackhole.consume(result);
    return connection;
  }

  @Benchmark
  public Object get_phm(BenchmarkPhmGetState state, Blackhole blackhole) throws IOException {
    int result = 0;
    ImageKey[] keys = state.keys;
    PersistentHashMap<ImageKey, ImageValue> map = state.map;
    for (ImageKey key : keys) {
      if (map.get(key) == null) {
        result ^= 1;
      }
    }
    blackhole.consume(result);
    return map;
  }

  @Benchmark
  public Object get_mvstore(BenchmarkMvstoreGetState state, Blackhole blackhole) {
    int result = 0;
    ImageKey[] keys = state.keys;
    var map = state.map;
    for (ImageKey key : keys) {
      if (map.get(key) == null) {
        result ^= 1;
      }
    }
    blackhole.consume(result);
    return map;
  }

  //@Benchmark
  //public Object put(BaseBenchmarkState.ObjectPutOrRemoveBenchmarkState state, Blackhole blackhole) {
  //  HashMap<ArbitraryPojo, ArbitraryPojo> map = new HashMap<>(0, state.loadFactor);
  //  for (ArbitraryPojo key : state.keys) {
  //    map.put(key, key);
  //  }
  //  blackhole.consume(map.size());
  //  for (ArbitraryPojo key : state.keys2) {
  //    map.put(key, key);
  //  }
  //  blackhole.consume(map.size());
  //  return map;
  //}
  //
  //@SuppressWarnings("DuplicatedCode")
  //@Benchmark
  //public Object remove(BaseBenchmarkState.ObjectPutOrRemoveBenchmarkState state, Blackhole blackhole) {
  //  HashMap<ArbitraryPojo, ArbitraryPojo> map = new HashMap<>(0, state.loadFactor);
  //  int add = 0;
  //  int remove = 0;
  //  ArbitraryPojo[] keys = state.keys;
  //  ArbitraryPojo[] keys2 = state.keys2;
  //  while (add < keys.length) {
  //    map.put(keys[add], keys[add]);
  //    add++;
  //    map.put(keys[add], keys[add]);
  //    add++;
  //    map.remove(keys2[remove++]);
  //  }
  //  blackhole.consume(map.size());
  //  return map;
  //}
}