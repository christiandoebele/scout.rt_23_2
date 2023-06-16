/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.shared.services.common.calendar;

import java.util.Objects;
import java.util.Random;

public class CalendarDescriptor implements ICalendarDescriptor {

  private long m_calendarId;
  private String m_name;
  private boolean m_visible;
  private String m_cssClass;

  public CalendarDescriptor(String name) {
    this(new Random().nextInt(), name);
  }

  public CalendarDescriptor(long calendarId, String name) {
    this(calendarId, name, true);
  }

  public CalendarDescriptor(long calendarId, String name, boolean visible) {
    setCalendarId(calendarId);
    m_name = name;
    m_visible = visible;
  }

  public CalendarDescriptor(long calendarId, String name, boolean visible, String cssClass) {
    setCalendarId(calendarId);
    m_name = name;
    m_visible = visible;
    m_cssClass = cssClass;
  }

  @Override
  public long getCalendarId() {
    return m_calendarId;
  }

  @Override
  public void setCalendarId(long calendarId) {
    if (calendarId == 0) {
      throw new IllegalArgumentException("Can not set calendarId. The value 0 is a reserved value");
    }
    m_calendarId = calendarId;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public void setName(String name) {
    m_name = name;
  }

  @Override
  public String getCssClass() {
    return m_cssClass;
  }

  @Override
  public void setCssClass(String cssClass) {
    m_cssClass = cssClass;
  }

  @Override
  public boolean isVisible() {
    return m_visible;
  }

  @Override
  public void setVisible(boolean visible) {
    m_visible = visible;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CalendarDescriptor that = (CalendarDescriptor) o;

    if (m_calendarId != that.m_calendarId) {
      return false;
    }
    if (m_visible != that.m_visible) {
      return false;
    }
    if (!Objects.equals(m_name, that.m_name)) {
      return false;
    }
    if (!Objects.equals(m_cssClass, that.m_cssClass)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = (int) (m_calendarId ^ (m_calendarId >>> 32));
    result = 31 * result + (m_name != null ? m_name.hashCode() : 0);
    result = 31 * result + (m_visible ? 1 : 0);
    result = 31 * result + (m_cssClass != null ? m_cssClass.hashCode() : 0);
    return result;
  }
}
