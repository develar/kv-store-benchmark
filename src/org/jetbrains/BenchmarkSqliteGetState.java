package org.jetbrains;

import com.intellij.openapi.util.io.NioFiles;
import org.jetbrains.sqlite.SQLiteConfig;
import org.jetbrains.sqlite.SqliteConnection;
import org.openjdk.jmh.annotations.*;

import java.nio.file.Files;
import java.nio.file.Path;

@State(Scope.Thread)
public class BenchmarkSqliteGetState {
  public SqliteConnection connection;
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

    connection = new SqliteConnection(dir.resolve("test.db"), new SQLiteConfig());
    connection.beginTransaction();

    //connection.execute("pragma page_size = 8192");
    connection.execute("pragma cache_size = 2000");

    connection.execute("drop table if exists data");
    connection.execute("create table data (contentDigest integer not null, contentLength integer not null, w integer not null, h integer not null, data blob not null, PRIMARY KEY(contentDigest, contentLength)) strict");
    SqliteTest.fillDatabase(connection, keys, values);
    connection.commit();

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
