package io.github.leawind.gitparcel.common.utils.algorithms;

import io.github.leawind.gitparcel.common.testutils.GitParcelRandom;
import java.util.concurrent.TimeUnit;

import io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLE;
import net.minecraft.core.Vec3i;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
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
