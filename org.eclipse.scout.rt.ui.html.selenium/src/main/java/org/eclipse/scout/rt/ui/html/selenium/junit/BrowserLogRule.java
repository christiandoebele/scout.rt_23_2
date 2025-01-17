/*
 * Copyright (c) 2010, 2025 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.ui.html.selenium.junit;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.scout.rt.ui.html.selenium.util.SeleniumDriver;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures and outputs the browser log. Requires setup of selenium logging preferences, see {@link SeleniumDriver}.
 */
public class BrowserLogRule extends TestWatcher {
  private static final Logger LOG = LoggerFactory.getLogger(BrowserLogRule.class);

  private final SimpleDateFormat m_dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  private final WebDriver m_driver;
  private Date m_start;

  public BrowserLogRule(AbstractSeleniumTest test) {
    m_driver = test.getDriver();
  }

  @Override
  protected void starting(Description description) {
    m_start = new Date();
    LOG.info("starting {}.{}", description.getClassName(), description.getMethodName());
  }

  @Override
  protected void finished(Description description) {
    Date end = new Date();
    long duration = end.getTime() - m_start.getTime();

    StringBuilder sb = new StringBuilder();
    LogEntries logEntries = m_driver.manage().logs().get(LogType.BROWSER);
    for (LogEntry logEntry : logEntries) {
      sb.append(m_dateFormat.format(new Date(logEntry.getTimestamp()))).append(" ").append(logEntry.getLevel()).append(" ").append(logEntry.getMessage()).append("\n");
    }
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1);
      LOG.info("browser log:\n{}", sb);
    }
    LOG.info("finished {}.{} (took {} ms)", description.getClassName(), description.getMethodName(), duration);
  }
}
