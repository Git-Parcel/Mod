package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@SuppressWarnings("unused")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
public class SubdivideAlgoBenchmark {

  @Param({"2"})
  private int variance;

  private Parcel parcel;
  private Function<BlockPos, Integer> values;
  private SubdivideAlgo.ResultFactory<SubdivideAlgoTest.ParcelWithValue> factory;

  @Setup
  public void setup() {
    var random = new RandomForMC(12138);

    int size = 16;
    parcel = new Parcel(0, 0, 0, size, size, size);
    factory = SubdivideAlgoTest.ParcelWithValue::new;

    var testCase = SubdivideAlgoTest.TestCase.create(random, parcel, variance);
    values = testCase::get;
  }

  @Benchmark
  public void benchmarkV1(Blackhole bh) {
    var result = SubdivideAlgo.V1.subdivide(parcel, values, factory);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkV2(Blackhole bh) {
    var result = SubdivideAlgo.V2.subdivide(parcel, values, factory);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkV3(Blackhole bh) {
    var result = SubdivideAlgo.V3.subdivide(parcel, values, factory);
    bh.consume(result);
  }
}
