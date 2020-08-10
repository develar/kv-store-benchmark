// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.jetbrains.mvstore.DataUtil;
import org.jetbrains.mvstore.type.DataType;

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
    result = 31 * result + (width != +0.0f ? Float.floatToIntBits(width) : 0);
    result = 31 * result + (height != +0.0f ? Float.floatToIntBits(height) : 0);
    result = 31 * result + actualWidth;
    result = 31 * result + actualHeight;
    return result;
  }

  static final class ImageValueSerializer implements DataType<ImageValue> {
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
    public void write(ByteBuf buf, ImageValue obj) {
      buf.writeFloat(obj.width);
      buf.writeFloat(obj.height);
      DataUtil.writeVarInt(buf, obj.actualWidth);
      DataUtil.writeVarInt(buf, obj.actualHeight);

      DataUtil.writeByteArray(buf, obj.data);
    }

    @Override
    public ImageValue read(ByteBuf buf) {
      float width = buf.readFloat();
      float height = buf.readFloat();
      int actualWidth = DataUtil.readVarInt(buf);
      int actualHeight = DataUtil.readVarInt(buf);

      int length = DataUtil.readVarInt(buf);
      if (length > MAX_IMAGE_SIZE) {
        throw new IllegalStateException("Size of data is too big: " + length);
      }

      byte[] obj = ByteBufUtil.getBytes(buf, buf.readerIndex(), length);
      buf.readerIndex(buf.readerIndex() + length);
      return new ImageValue(obj, width, height, actualWidth, actualHeight);
    }
  }
}
