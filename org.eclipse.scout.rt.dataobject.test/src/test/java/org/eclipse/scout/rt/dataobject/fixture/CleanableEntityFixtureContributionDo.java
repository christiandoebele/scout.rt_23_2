/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.dataobject.fixture;

import org.eclipse.scout.rt.dataobject.ContributesTo;
import org.eclipse.scout.rt.dataobject.DoNode;
import org.eclipse.scout.rt.dataobject.ICleanableDataObject;
import org.eclipse.scout.rt.dataobject.IDoEntityContribution;

@ContributesTo(CleanableEntityFixtureDo.class)
public final class CleanableEntityFixtureContributionDo extends EntityFixtureDo implements ICleanableDataObject, IDoEntityContribution {

  @Override
  public boolean isNodeCleanable(DoNode<?> node) {
    // Make other entity clearable
    if (node.equals(otherEntity())) {
      return true;
    }
    return ICleanableDataObject.super.isNodeCleanable(node);
  }
}
