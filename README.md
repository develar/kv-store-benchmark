## Running Benchmark
```
mvn package

java --add-opens=java.base/java.io=ALL-UNNAMED -cp target/benchmarks.jar:lib/h2-mvstore.jar:lib/3rd-party-rt.jar:lib/app.jar:lib/util.jar:lib/util_rt.jar org.openjdk.jmh.Main -bm ss -wi 40 -i 40 -f 1 -foe true -rf json \
  -p mapSize=100,1K,10K,100K -p cacheSize=16,64 -tu ms
```

[Git LFS](https://git-lfs.com) is required to download the `lib` directory. Use `git lfs checkout` to check out after Git LFS installation.