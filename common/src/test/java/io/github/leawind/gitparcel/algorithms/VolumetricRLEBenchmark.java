package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.api.parcel.Parcel;
import io.github.leawind.gitparcel.testutils.RandomForMC;
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

  private Parcel parcel;
  private VolumetricRLE.ValueGetter values;

  @Setup
  public void setup() {
    var random = new RandomForMC(12138);
    var size = new Vec3i(this.size, this.size, this.size);

    values = new VolumetricRLETest.TestedValues(size, variance, random);
  }

  @Benchmark
  public void benchmarkV2(Blackhole bh) {
    var result = VolumetricRLE.IMPL.encode(parcel.sizeX, parcel.sizeY, parcel.sizeZ, values);
    bh.consume(result);
  }
}
