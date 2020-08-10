// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.mvstore.DataUtil;
import org.jetbrains.mvstore.type.KeyableDataType;

final class ImageKey implements Comparable<ImageKey> {
  long contentDigest;
  // add image size to prevent collision
  final int contentLength;

  ImageKey(long contentDigest, int contentLength) {
    this.contentDigest = contentDigest;
    this.contentLength = contentLength;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ImageKey key = (ImageKey)o;
    return contentDigest == key.contentDigest && contentLength == key.contentLength;
  }

  @Override
  public int hashCode() {
    int result = (int)(contentDigest ^ (contentDigest >>> 32));
    result = 31 * result + contentLength;
    return result;
  }

  @Override
  public int compareTo(@NotNull ImageKey o) {
    if (contentDigest != o.contentDigest) {
      return contentDigest < o.contentDigest ? -1 : 1;
    }
    return Integer.compare(contentLength, o.contentLength);
  }

  static final class ImageKeySerializer implements KeyableDataType<ImageKey> {
    @Override
    public int compare(ImageKey a, ImageKey b) {
      return a.compareTo(b);
    }

    @Override
    public int getMemory(ImageKey obj) {
      return DataUtil.VAR_LONG_MAX_SIZE + DataUtil.VAR_INT_MAX_SIZE;
    }

    @Override
    public void write(ByteBuf buf, ImageKey obj) {
      DataUtil.writeVarLong(buf, obj.contentDigest);
      DataUtil.writeVarInt(buf, obj.contentLength);
    }

    @Override
    public ImageKey read(ByteBuf buff) {
      return new ImageKey(DataUtil.readVarLong(buff), DataUtil.readVarInt(buff));
    }

    @Override
    public ImageKey[] createStorage(int size) {
      return new ImageKey[size];
    }
  }
}
