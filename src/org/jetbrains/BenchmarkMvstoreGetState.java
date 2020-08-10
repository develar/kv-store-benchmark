package org.jetbrains;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.mvstore.MVMap;
import org.jetbrains.mvstore.MVStore;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("DuplicatedCode")
@State(Scope.Thread)
public class BenchmarkMvstoreGetState extends BaseBenchmarkState {
  public MVMap<ImageKey, ImageValue> map;
  ImageKey[] keys;
  private Path file;

  @Override
  @Setup
  public void setup() throws Exception {
    file = Files.createTempFile("", "db.db");
    ImageKey[] keys = Util.loadObjectArray(mapSize);
    ImageValue[] values = Util.generateValues(mapSize);
    MVStore store = openStore();
    MVMap.Builder<ImageKey, ImageValue> mapBuilder = new MVMap.Builder<ImageKey, ImageValue>()
      .keyType(new ImageKey.ImageKeySerializer())
      .valueType(new ImageValue.ImageValueSerializer());
    MVMap<ImageKey, ImageValue> map = store.openMap("icons", mapBuilder);

    for (int i = 0, l = keys.length; i < l; i++) {
      // for non-identity maps with object keys we use a distinct set of keys (the different object with the same value is used for successful “get” calls).
      ImageKey key = keys[i];
      ImageKey newKey = new ImageKey(key.contentDigest, key.contentLength);
      if (i % oneFailureOutOf == 0) {
        newKey.contentDigest = i;
      }
      map.put(newKey, values[i]);
    }

    store.commit();
    store.compact(90, 1024 * 1024);
    store.close();
    store = openStore();
    map = store.openMap("icons", mapBuilder);

    this.keys = keys;
    this.map = map;
  }

  @NotNull
  private MVStore openStore() throws IOException {
    return new MVStore.Builder()
      .compressionLevel(compression)
      .autoCommitDisabled()
      .open(file);
  }

  @TearDown
  public void tearDown() throws Exception {
    if (file != null) {
      Files.deleteIfExists(file);
      file = null;
    }
  }
}
