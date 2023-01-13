package org.jetbrains;

import com.intellij.openapi.util.io.NioFiles;
import org.jetbrains.sqlite.*;

import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("DuplicatedCode")
public final class SqliteTest {
  public static void main(String[] args) throws Exception {
    Path dbDir = Files.createTempDirectory("db");
    try {
      String file = dbDir.toString() + "/test.db";
      var connection = new SqliteConnection(Path.of(file), new SQLiteConfig());
      try {
        connection.beginTransaction();
        connection.execute("drop table if exists data");
        connection.execute("create table data (contentDigest integer, contentLength integer, w integer, h integer, data blob)");
        connection.execute("create index key_idx ON data (contentDigest, contentLength)");

        String size = "100";
        ImageKey[] keys = Util.loadObjectArray(size);
        ImageValue[] values = Util.generateValues(size);
        fillDatabase(connection, keys, values);
        connection.commit();

        var binder = new LongBinder(2, 1);
        try (var statement = connection.prepareStatement("select data from data where contentDigest = ? and contentLength = ?", binder)) {
          long start = System.currentTimeMillis();
          lookup(keys[keys.length / 2 + 1], statement, binder);
          long end = System.currentTimeMillis();
          System.out.println(end - start);
        }
      }
      finally {
        connection.close();
      }
    }
    finally {
      NioFiles.deleteRecursively(dbDir);
    }
  }

  private static void lookup(ImageKey key, SqlitePreparedStatement statement, LongBinder binder) {
    binder.bind(key.contentDigest, key.contentLength);
    var resultSet = statement.executeQuery();
    if (resultSet.next()) {
      byte[] blob = resultSet.getBytes(1);
      if (blob.length == 0) {
       @SuppressWarnings("unused") int t = 0;
      }
    }
  }

  public static void fillDatabase(SqliteConnection connection, ImageKey[] keys, ImageValue[] values) {
    var binder = new ObjectBinder(5, Math.min(keys.length, 10_000));
    var statement = connection.prepareStatement("insert into data values(?, ?, ?, ?, ?)", binder);
    int batchCount = 0;
    for (int i = 0, l = keys.length; i < l; i++) {
      ImageKey key = keys[i];
      ImageKey newKey = new ImageKey(key.contentDigest, key.contentLength);
      if (i % 2 == 0) {
        newKey.contentDigest = i;
      }
      ImageValue value = values[i];

      binder.bind(newKey.contentDigest, newKey.contentLength, value.actualWidth, value.actualHeight, value.data);
      binder.addBatch();

      if (batchCount++ > 10_000) {
        statement.executeBatch();
        batchCount = 0;
      }
    }

    if (batchCount > 0) {
      statement.executeBatch();
    }

    statement.close();

    //vacuum(connection);
  }

  //private static void vacuum(Connection connection) throws SQLException {
  //  Statement statement = connection.createStatement();
  //  statement.execute("vacuum test");
  //  statement.close();
  //  connection.commit();
  //}
}
