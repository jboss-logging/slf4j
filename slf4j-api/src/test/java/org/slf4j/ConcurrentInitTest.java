/**
 * Copyright 2013  Simon Arlott
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.slf4j;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.helpers.NOPLoggerFactory;
import org.slf4j.helpers.SubstituteLoggerFactory;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ConcurrentInitTest {
  private static class GetLogger implements Callable<ILoggerFactory> {
    private CyclicBarrier barrier;

    public GetLogger(CyclicBarrier barrier) {
      this.barrier = barrier;
    }

    public ILoggerFactory call() throws InterruptedException, BrokenBarrierException {
      barrier.await();
      return LoggerFactory.getILoggerFactory();
    }
  }

  @Test
  public void getLogger() throws InterruptedException, BrokenBarrierException, ExecutionException {
    // Create a lot of threads that will all request the logger factory
    @SuppressWarnings("unchecked")
    FutureTask<ILoggerFactory>[] tasks = new FutureTask[10 + Runtime.getRuntime().availableProcessors() * 2];
    CyclicBarrier barrier = new CyclicBarrier(tasks.length + 1);
    try {
      for (int i = 0; i < tasks.length; i++) {
        tasks[i] = new FutureTask<ILoggerFactory>(new GetLogger(barrier));

        // Each thread will block on the barrier
        new Thread(tasks[i]).start();
      }

      // Wait for all threads to be at the barrier and allow them to run
      // all at the same time
      barrier.await();
    } finally {
      barrier.reset();
    }

    for (int i = 0; i < tasks.length; i++) {
      // If any of the logger factories are SubstituteLoggerFactory then
      // initialisation failed
      Assert.assertFalse(i + "=" + tasks[i].get().getClass().getName(),
          tasks[i].get() instanceof SubstituteLoggerFactory);
      Assert.assertTrue(i + "=" + tasks[i].get().getClass().getName(),
          tasks[i].get() instanceof NOPLoggerFactory);
    }
  }
}
