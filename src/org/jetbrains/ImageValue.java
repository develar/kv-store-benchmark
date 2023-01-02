// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetbrains.mvstore.DataUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class ImageValue {
  final byte[] data;
  final float width;
  final float height;
  final int actualWidth;
  final int actualHeight;

  ImageValue(byte[] data, float width, float height, int actualWidth, int actualHeight) {
    this.data = data;
    this.width = width;
    this.height = height;
    this.actualWidth = actualWidth;
    this.actualHeight = actualHeight;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ImageValue value = (ImageValue)o;
    return Float.compare(value.width, width) == 0 &&
           Float.compare(value.height, height) == 0 &&
           actualWidth == value.actualWidth &&
           actualHeight == value.actualHeight && Arrays.equals(data, value.data);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(data);
    result = 31 * result + (width != 0.0f ? Float.floatToIntBits(width) : 0);
    result = 31 * result + (height != 0.0f ? Float.floatToIntBits(height) : 0);
    result = 31 * result + actualWidth;
    result = 31 * result + actualHeight;
    return result;
  }

  static final class ImageValueSerializer extends BasicDataType<ImageValue> {
    private static final long MAX_IMAGE_SIZE = 16 * 1024 * 1024;

    @Override
    public ImageValue[] createStorage(int size) {
      return new ImageValue[size];
    }

    @Override
    public int getMemory(ImageValue obj) {
      return Float.BYTES * 2 +
             4 /* w or h var int size (strictly speaking 5, but 4 is totally ok) */ * 2 +
             DataUtil.VAR_INT_MAX_SIZE /* max var int size */ + obj.data.length;
    }

    @Override
    public void write(WriteBuffer buff, ImageValue obj) {
      buff.putFloat(obj.width);
      buff.putFloat(obj.height);
      buff.putVarInt(obj.actualWidth);
      buff.putVarInt(obj.actualHeight);
      buff.putVarInt(obj.data.length);
      buff.put(obj.data);
    }

    @Override
    public ImageValue read(ByteBuffer buff) {
      float width = buff.getFloat();
      float height = buff.getFloat();
      int actualWidth = DataUtils.readVarInt(buff);
      int actualHeight = DataUtils.readVarInt(buff);

      int length = DataUtils.readVarInt(buff);
      if (length > MAX_IMAGE_SIZE) {
        throw new IllegalStateException("Size of data is too big: " + length);
      }

      byte[] obj = new byte[length];
      buff.get(obj);
      return new ImageValue(obj, width, height, actualWidth, actualHeight);
    }
  }
}
