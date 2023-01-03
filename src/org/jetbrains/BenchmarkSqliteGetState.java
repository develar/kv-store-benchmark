package org.jetbrains;

import com.intellij.openapi.util.io.NioFiles;
import org.openjdk.jmh.annotations.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@State(Scope.Thread)
public class BenchmarkSqliteGetState {
  public Connection connection;
  ImageKey[] keys;
  private Path dir;

  @Param("1K")
  public String mapSize;

  @Param("2")
  public int oneFailureOutOf;

  @Setup
  public void setup() throws Exception {
    dir = Files.createTempDirectory("sqlite");
    ImageKey[] keys = Util.loadObjectArray(mapSize);
    ImageValue[] values = Util.generateValues(mapSize);

    connection = DriverManager.getConnection("jdbc:sqlite:" + dir.toString() + "/test.db");
    connection.setAutoCommit(false);
    Statement createStatement = connection.createStatement();
    createStatement.setQueryTimeout(30);
    createStatement.executeUpdate("drop table if exists data");
    createStatement.executeUpdate("create table data (contentDigest integer, contentLength integer, w integer, h integer, data blob)");
    createStatement.executeUpdate("create index key_idx ON data (contentDigest, contentLength)");

    SqliteTest.fillDatabase(connection, keys, values);

    this.keys = keys;
  }

  @TearDown
  public void tearDown() throws Exception {
    if (connection != null) {
      connection.close();
    }

    if (dir != null) {
      NioFiles.deleteRecursively(dir);
      dir = null;
    }
  }
}
