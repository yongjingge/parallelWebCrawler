package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState profilingState;
  private final Object targetObject;

  // Add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, ProfilingState profilingState, Object targetObject) {
    this.clock = Objects.requireNonNull(clock);
    this.profilingState = Objects.requireNonNull(profilingState);
    this.targetObject = Objects.requireNonNull(targetObject);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    /**
     * This method interceptor should inspect the called method to see if it is a @Profiled annotated method.
     * For profiled methods, the interceptor should record the start time,
     * then invoke the method using the profiled object.
     * Finally, for profiled methods, the interceptor should record the method using the ProfilingState. */
    if (method.getDeclaringClass().equals(Object.class) && method.getAnnotation(Profiled.class) != null) {
      final Instant start = clock.instant();
      try {
        method.invoke(targetObject, args);
      } catch (InvocationTargetException invocationTargetException) {
        throw invocationTargetException.getTargetException();
      } catch (IllegalAccessException illegalAccessException) {
        throw new RuntimeException(illegalAccessException);
      } finally {
        profilingState.record(targetObject.getClass(), method, Duration.between(start, clock.instant()));
      }
    }
    return method.invoke(targetObject, args);
  }
}
