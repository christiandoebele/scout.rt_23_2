/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.dataobject;

import org.eclipse.scout.rt.platform.BEANS;

public interface ICleanableDataObject {

  /**
   * Returns true, if the provided node has no relevant value and can be removed from the json structure.
   *
   * @param node
   *     the node to be checked
   * @return true when the node can be cleaned
   */
  default boolean isNodeCleanable(DoNode<?> node) {
    return BEANS.get(DataObjectHelper.class).isNodeCleanable(node);
  }
}
