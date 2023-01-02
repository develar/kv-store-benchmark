package org.jetbrains;

import com.intellij.openapi.util.io.NioFiles;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@State(Scope.Thread)
public class BenchmarkPhmGetState extends BaseBenchmarkState {
  public PersistentHashMap<ImageKey, ImageValue> map;
  ImageKey[] keys;
  private Path dir;

  @Override
  @Setup
  public void setup() throws Exception {
    System.setProperty("idea.compression.enabled", compression > 0 ? "true" : "false");

    dir = Files.createTempDirectory("phm");
    ImageKey[] keys = Util.loadObjectArray(mapSize);
    ImageValue[] values = Util.generateValues(mapSize);
    PersistentHashMap<ImageKey, ImageValue> map = createMap();

    for (int i = 0, l = keys.length; i < l; i++) {
      // for non-identity maps with object keys we use a distinct set of keys (the different object with the same value is used for successful “get” calls).
      ImageKey key = keys[i];
      ImageKey newKey = new ImageKey(key.contentDigest, key.contentLength);
      if (i % oneFailureOutOf == 0) {
        newKey.contentDigest = i;
      }
      map.put(newKey, values[i]);
    }

    map.dropMemoryCaches();
    map.close();
    map = createMap();

    this.keys = keys;
    this.map = map;
  }

  @NotNull
  private PersistentHashMap<ImageKey, ImageValue> createMap() throws IOException {
    return new PersistentHashMap<>(dir.resolve("db.db"), new KeyDescriptor<>() {
      @Override
      public int getHashCode(ImageKey image) {
        return image.hashCode();
      }

      @Override
      public void save(@NotNull DataOutput dataOutput, ImageKey image) throws IOException {
        dataOutput.writeLong(image.contentDigest);
        dataOutput.writeInt(image.contentLength);
      }

      @Override
      public boolean isEqual(ImageKey a, ImageKey b) {
        return Objects.equals(a, b);
      }

      @Override
      public ImageKey read(@NotNull DataInput dataInput) throws IOException {
        return new ImageKey(dataInput.readLong(), dataInput.readInt());
      }
    }, new DataExternalizer<>() {
      @Override
      public void save(@NotNull DataOutput dataOutput, ImageValue imageValue) throws IOException {
        dataOutput.writeFloat(imageValue.width);
        dataOutput.writeFloat(imageValue.height);
        dataOutput.writeInt(imageValue.actualWidth);
        dataOutput.writeInt(imageValue.actualHeight);
        dataOutput.writeInt(imageValue.data.length);
        dataOutput.write(imageValue.data);
      }

      @Override
      public ImageValue read(@NotNull DataInput dataInput) throws IOException {
        float w = dataInput.readFloat();
        float h = dataInput.readFloat();
        int aW = dataInput.readInt();
        int aH = dataInput.readInt();
        byte[] data = new byte[dataInput.readInt()];
        dataInput.readFully(data);
        return new ImageValue(data, w, h, aW, aH);
      }
    });
  }

  @TearDown
  public void tearDown() throws Exception {
    if (dir != null) {
      NioFiles.deleteRecursively(dir);
      dir = null;
    }
  }
}
