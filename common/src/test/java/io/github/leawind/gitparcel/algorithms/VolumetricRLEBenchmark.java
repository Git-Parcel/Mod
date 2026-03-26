package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.testutils.GitParcelRandom;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.Vec3i;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@SuppressWarnings("unused")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 3)
public class VolumetricRLEBenchmark {

  @Param({"2", "4", "8"})
  private int variance;

  @Param({"16", "32", "64"})
  private int size;

  private VolumetricRLE.ValueGetter values;

  @Setup
  public void setup() {
    var random = new GitParcelRandom(12138);
    values = new VolumetricRLETest.TestedValues(new Vec3i(size, size, size), variance, random);
  }

  @Benchmark
  public void benchmarkV2(Blackhole bh) {
    var result = VolumetricRLE.IMPL.encode(size, size, size, values);
    bh.consume(result);
  }
}
