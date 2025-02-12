/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.shared;

import java.util.Calendar;

public final class OfficialVersion {

  private OfficialVersion() {
  }

  public static final String COPYRIGHT_VERSION = "24.2";
  public static final String COPYRIGHT = "Scout " + COPYRIGHT_VERSION + ", &copy; BSI Business Systems Integration AG " + 2001 + "," + Calendar.getInstance().get(Calendar.YEAR) + " EPL";
}
