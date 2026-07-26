package io.github.leawind.gitparcel.common.api.operation;

import java.util.concurrent.Callable;

/** Bridges the world-access phases of a background operation onto the Minecraft server thread. */
public interface ServerThreadBridge {
  ServerThreadBridge DIRECT =
      new ServerThreadBridge() {
        @Override
        public <T> T call(Callable<T> action) throws Exception {
          return action.call();
        }
      };

  <T> T call(Callable<T> action) throws Exception;

  default void run(CheckedRunnable action) throws Exception {
    call(
        () -> {
          action.run();
          return null;
        });
  }

  @FunctionalInterface
  interface CheckedRunnable {
    void run() throws Exception;
  }
}
