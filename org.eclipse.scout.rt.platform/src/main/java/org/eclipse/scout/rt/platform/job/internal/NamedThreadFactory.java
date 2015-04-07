/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.job.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.job.JobExceptionHandler;

/**
 * Thread factory for named threads and to handle uncaught exceptions.
 *
 * @since 5.1
 */
public class NamedThreadFactory implements ThreadFactory, UncaughtExceptionHandler {

  protected static final IScoutLogger LOG = ScoutLogManager.getLogger(NamedThreadFactory.class);

  private final AtomicLong m_sequence;
  private final String m_threadName;
  private final ThreadGroup m_group;

  public NamedThreadFactory(final String threadName) {
    m_threadName = threadName;
    m_sequence = new AtomicLong();

    final SecurityManager securityManager = System.getSecurityManager();
    m_group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
  }

  @Override
  public Thread newThread(final Runnable runnable) {
    final ThreadInfo threadInfo = new ThreadInfo(m_threadName, m_sequence.incrementAndGet());

    final Thread thread = new Thread(m_group, runnable, threadInfo.buildThreadName(), 0) {

      @Override
      public void run() {
        ThreadInfo.CURRENT.set(threadInfo);
        try {
          super.run();
        }
        finally {
          ThreadInfo.CURRENT.remove();
        }
      }
    };

    if (thread.isDaemon()) {
      thread.setDaemon(false);
    }
    if (thread.getPriority() != Thread.NORM_PRIORITY) {
      thread.setPriority(Thread.NORM_PRIORITY);
    }

    thread.setUncaughtExceptionHandler(this);

    return thread;
  }

  // === UncaughtExceptionHandler ===

  @Override
  public void uncaughtException(final Thread thread, final Throwable t) {
    try {
      BEANS.get(JobExceptionHandler.class).handleUncaughtException(thread, t);
    }
    catch (final RuntimeException e) {
      LOG.error(String.format("Failed to handle uncaught exception [thread=%s, cause=%s]", thread.getName(), t), e);
    }
  }

  /**
   * Information about the worker thread.
   */
  public static class ThreadInfo {

    /**
     * The {@link ThreadInfo} which is currently associated with the current thread.
     */
    public static final ThreadLocal<ThreadInfo> CURRENT = new ThreadLocal<>();

    private final String m_originalThreadName;
    private final long m_sequence;

    private volatile String m_currentThreadName;
    private volatile String m_currentJobName;
    private volatile JobState m_currentJobState;
    private volatile String m_currentJobStateInfo;

    public ThreadInfo(final String threadName, final long sequence) {
      m_originalThreadName = threadName;
      m_sequence = sequence;
      m_currentJobState = JobState.Idle;
    }

    /**
     * Invoke to update the thread's state.
     *
     * @param jobState
     *          new state of the current job.
     * @param jobStateInfo
     *          information associated with the new job state.
     */
    public void updateState(final JobState jobState, final String jobStateInfo) {
      m_currentJobStateInfo = jobStateInfo;
      m_currentJobState = jobState;
      Thread.currentThread().setName(buildThreadName());
    }

    /**
     * Invoke to update the thread's name and state.
     *
     * @param threadName
     *          new thread name, or <code>null</code> to apply the thread's original name.
     * @param jobName
     *          name of the current job.
     * @param state
     *          state of the current job.
     */
    public void updateNameAndState(final String threadName, final String jobName, final JobState state) {
      m_currentThreadName = threadName;
      m_currentJobName = jobName;
      m_currentJobState = state;
      m_currentJobStateInfo = null;
      Thread.currentThread().setName(buildThreadName());
    }

    private String buildThreadName() {
      final StringWriter writer = new StringWriter();
      final PrintWriter out = new PrintWriter(writer);

      out.print(StringUtility.nvl(m_currentThreadName, m_originalThreadName));
      out.print("-");
      out.print(m_sequence);
      if (m_currentJobState != null) {
        out.print(" [");
        out.print(m_currentJobState);
        if (StringUtility.hasText(m_currentJobStateInfo)) {
          out.printf(" '%s'", m_currentJobStateInfo);
        }
        out.print("]");
      }
      if (StringUtility.hasText(m_currentJobName)) {
        out.printf(" %s", m_currentJobName);
      }

      return writer.toString();
    }
  }

  public static enum JobState {
    Idle, Running, Blocked, Resuming;
  }
}
