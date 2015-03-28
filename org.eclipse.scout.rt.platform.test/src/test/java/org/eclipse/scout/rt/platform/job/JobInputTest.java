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
package org.eclipse.scout.rt.platform.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.eclipse.scout.commons.nls.NlsLocale;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class JobInputTest {

  @Before
  public void before() {
    NlsLocale.CURRENT.remove();
  }

  @Test
  public void testCopy() {
    JobInput input = Jobs.newInput(RunContexts.empty());
    input.name("name");
    input.id("123");

    JobInput copy = input.copy();

    assertNotSame(input.runContext(), copy.runContext());
    assertEquals(input.name(), copy.name());
    assertEquals(input.id(), copy.id());
  }

  @Test
  public void testFillCurrentName() {
    assertNull(Jobs.newInput(RunContexts.copyCurrent()).name());
    assertEquals("ABC", Jobs.newInput(RunContexts.copyCurrent()).name("ABC").name());
  }

  @Test
  public void testFillCurrentId() {
    assertNull(Jobs.newInput(RunContexts.copyCurrent()).id());
    assertEquals("123", Jobs.newInput(RunContexts.copyCurrent()).id("123").id());
  }
}
