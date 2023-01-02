// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jetbrains.mvstore.DataUtil;

import java.nio.ByteBuffer;

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
  public int compareTo(ImageKey o) {
    if (o == null) {
      return 1;
    }

    if (contentDigest != o.contentDigest) {
      return contentDigest < o.contentDigest ? -1 : 1;
    }
    return Integer.compare(contentLength, o.contentLength);
  }

  static final class ImageKeySerializer extends BasicDataType<ImageKey> {
    @Override
    public int compare(ImageKey a, ImageKey b) {
      return a.compareTo(b);
    }

    @Override
    public int getMemory(ImageKey obj) {
      return DataUtil.VAR_LONG_MAX_SIZE + DataUtil.VAR_INT_MAX_SIZE;
    }

    @Override
    public void write(WriteBuffer buf, ImageKey obj) {
      buf.putVarLong(obj.contentDigest);
      buf.putVarInt(obj.contentLength);
    }

    @Override
    public void write(WriteBuffer buf, Object items, int len) {
      for (int i = 0; i < len; i++) {
          write(buf, ((ImageKey[])items)[i]);
      }
    }

    @Override
    public ImageKey read(ByteBuffer buf) {
      return new ImageKey(DataUtils.readVarLong(buf), DataUtils.readVarInt(buf));
    }

    @Override
    public ImageKey[] createStorage(int size) {
      return new ImageKey[size];
    }
  }
}
