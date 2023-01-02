package org.jetbrains;

import com.intellij.openapi.util.io.NioFiles;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

@State(Scope.Thread)
public class BenchmarkSqliteGetState extends BaseBenchmarkState {
  public Connection connection;
  ImageKey[] keys;
  private Path dir;

  @Override
  @Setup
  public void setup() throws Exception {
    dir = Files.createTempDirectory("sqlite");
    ImageKey[] keys = Util.loadObjectArray(mapSize);
    ImageValue[] values = Util.generateValues(mapSize);

    connection = DriverManager.getConnection("jdbc:sqlite:" + dir.toString() + "/test.db");
    Statement createStatement = connection.createStatement();
    createStatement.setQueryTimeout(30);
    createStatement.executeUpdate("drop table if exists data");
    createStatement.executeUpdate("create table data (contentDigest integer, contentLength integer, w integer, h integer, data blob)");
    createStatement.executeUpdate("create index contentDigest_idx ON data (contentDigest)");
    createStatement.executeUpdate("create index contentLength_idx ON data (contentLength)");

    PreparedStatement statement = connection.prepareStatement("insert into data values(?, ?, ?, ?, ?)");
    for (int i = 0, l = keys.length; i < l; i++) {
      // for non-identity maps with object keys we use a distinct set of keys (the different object with the same value is used for successful “get” calls).
      ImageKey key = keys[i];
      ImageKey newKey = new ImageKey(key.contentDigest, key.contentLength);
      if (i % oneFailureOutOf == 0) {
        newKey.contentDigest = i;
      }
      ImageValue value = values[i];

      statement.setLong(1, newKey.contentDigest);
      statement.setLong(2, newKey.contentLength);
      statement.setInt(3, value.actualWidth);
      statement.setInt(4, value.actualHeight);
      statement.setBytes(5, value.data);
      statement.executeUpdate();
    }

    this.keys = keys;
  }

  @TearDown
  public void tearDown() throws Exception {
    if (dir != null) {
      NioFiles.deleteRecursively(dir);
      dir = null;
    }
  }
}
