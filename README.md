## Running Benchmark
```
export JAVA_HOME=~/Downloads/jbr/Contents/Home
mvn package

java -cp target/benchmarks.jar:lib/platform-util-ex.jar:lib/util.jar org.openjdk.jmh.Main -bm ss -wi 40 -i 40 -f 1 -foe true -rf json \
  -p mapSize=100,1K,10K,100K -tu ms
```