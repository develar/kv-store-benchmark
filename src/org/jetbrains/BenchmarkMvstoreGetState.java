package org.jetbrains;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;

import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("DuplicatedCode")
@State(Scope.Thread)
public class BenchmarkMvstoreGetState  {
  public MVMap<ImageKey, ImageValue> map;
  ImageKey[] keys;
  private Path file;

  @Param("48")
  public int keysPerPage;

  @Param("1K")
  public String mapSize;

  @Param("2")
  public int oneFailureOutOf;

  @Param("0")
  public int compression;

  @Param("16")
  public int cacheSize;

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

  private @NotNull MVStore openStore() {
    if (keysPerPage <= 0) {
      throw new IllegalArgumentException("keysPerPage=" + keysPerPage + " cannot be <= 0");
    }
    if (cacheSize < 0) {
      throw new IllegalArgumentException("cacheSize=" + cacheSize + " cannot be < 0");
    }

    MVStore.Builder builder = new MVStore.Builder()
      .fileName(file.toString())
      .keysPerPage(keysPerPage)
      .cacheSize(cacheSize)
      .autoCommitDisabled();
    if (compression != 0) {
      builder.compress();
    }
    return builder.open();
  }

  @TearDown
  public void tearDown() throws Exception {
    if (file != null) {
      Files.deleteIfExists(file);
      file = null;
    }
  }
}
