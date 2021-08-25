package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  private <T> boolean isProfiled(Class<T> klass) {
    return Arrays.stream(klass.getDeclaredMethods()).anyMatch(method -> method.getAnnotation(Profiled.class) != null);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    /**Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
     * ProfilingMethodInterceptor (InvocationHandler) and return a dynamic proxy from this method.
     * See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
     */
    if (!isProfiled(klass)) {
      throw new IllegalArgumentException("Not Profiled");
    }

    @SuppressWarnings("unchecked")
    T proxy = (T) Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class<?>[] {klass},
            new ProfilingMethodInterceptor(clock, state, delegate)
    );
    return proxy;
  }

  @Override
  public void writeData(Path path) throws IOException {
    /** Write the ProfilingState data to the given file path. If a file already exists at that path,
     * the new data should be appended to the existing file. */
    Objects.requireNonNull(path);
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      writeData(writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
