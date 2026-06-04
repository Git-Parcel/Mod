package io.github.leawind.gitparcel.core.bridge.math;

public interface MathBridge {
  record Vec3i(int x, int y, int z) {}

  record Vec3l(long x, long y, long z) {}

  record Vec3f(float x, float y, float z) {}

  record Vec3d(double x, double y, double z) {}
}
