package org.jetbrains;

import com.intellij.openapi.util.io.NioFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

@SuppressWarnings("DuplicatedCode")
public final class SqliteTest {
  public static void main(String[] args) throws Exception {
    Path dbDir = Files.createTempDirectory("db");
    try {
      var connection = DriverManager.getConnection("jdbc:sqlite:" + dbDir.toString() + "/test.db");
      connection.setAutoCommit(false);
      try {
        Statement createStatement = connection.createStatement();
        createStatement.setQueryTimeout(30);
        createStatement.executeUpdate("drop table if exists data");
        createStatement.executeUpdate("create table data (contentDigest integer, contentLength integer, w integer, h integer, data blob)");
        createStatement.executeUpdate("create index key_idx ON data (contentDigest, contentLength)");

        ImageKey[] keys = Util.loadObjectArray("100000");
        ImageValue[] values = Util.generateValues("100000");
        fillDatabase(connection, keys, values);
        PreparedStatement statement = connection.prepareStatement("select data from data where contentDigest = ? and contentLength = ?");
        lookup(keys[keys.length / 2 + 1], statement);
      }
      finally {
        connection.close();
      }
    }
    finally {
      NioFiles.deleteRecursively(dbDir);
    }
  }

  private static void lookup(ImageKey key, PreparedStatement statement) throws SQLException {
    statement.setLong(1, key.contentDigest);
    statement.setLong(2, key.contentLength);
    ResultSet resultSet = statement.executeQuery();
    if (resultSet.next()) {
      byte[] blob = resultSet.getBytes(1);
      if (blob.length == 0) {
       @SuppressWarnings("unused") int t = 0;
      }
    }
  }

  public static void fillDatabase(Connection connection, ImageKey[] keys, ImageValue[] values) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("insert into data values(?, ?, ?, ?, ?)");
    int batchCount = 0;
    for (int i = 0, l = keys.length; i < l; i++) {
      ImageKey key = keys[i];
      ImageKey newKey = new ImageKey(key.contentDigest, key.contentLength);
      if (i % 2 == 0) {
        newKey.contentDigest = i;
      }
      ImageValue value = values[i];

      statement.setLong(1, newKey.contentDigest);
      statement.setLong(2, newKey.contentLength);
      statement.setInt(3, value.actualWidth);
      statement.setInt(4, value.actualHeight);
      statement.setBytes(5, value.data);

      if (batchCount++ > 10_000) {
        statement.executeBatch();
        connection.commit();
        batchCount = 0;
      }
      else {
        statement.addBatch();
      }
    }

    if (batchCount > 0) {
      statement.executeBatch();
      connection.commit();
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
