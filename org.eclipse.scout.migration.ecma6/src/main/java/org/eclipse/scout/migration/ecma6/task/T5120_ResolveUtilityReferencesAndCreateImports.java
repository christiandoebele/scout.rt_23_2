/*
 * Copyright (c) 2010-2019 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.migration.ecma6.task;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.scout.migration.ecma6.PathFilters;
import org.eclipse.scout.migration.ecma6.PathInfo;
import org.eclipse.scout.migration.ecma6.WorkingCopy;
import org.eclipse.scout.migration.ecma6.context.Context;
import org.eclipse.scout.migration.ecma6.model.api.INamedElement;
import org.eclipse.scout.migration.ecma6.model.api.INamedElement.Type;
import org.eclipse.scout.migration.ecma6.model.old.JsFile;
import org.eclipse.scout.rt.platform.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Order(5120)
public class T5120_ResolveUtilityReferencesAndCreateImports extends AbstractTask {
  private static final Logger LOG = LoggerFactory.getLogger(T5120_ResolveUtilityReferencesAndCreateImports.class);

  @SuppressWarnings("unchecked")
  private Predicate<PathInfo> m_filter = PathFilters.and(PathFilters.inSrcMainJs(), PathFilters.withExtension("js"));

  @Override
  public boolean accept(PathInfo pathInfo, Context context) {
    return m_filter.test(pathInfo);
  }

  @Override
  public void process(PathInfo pathInfo, Context context) {
    WorkingCopy workingCopy = context.ensureWorkingCopy(pathInfo.getPath());

    String source = workingCopy.getSource();
    JsFile jsFile = context.ensureJsFile(workingCopy);

    List<INamedElement> elems = context.getApi().getElements(Type.Utility);
    elems.addAll(context.getLibraries().getElements(Type.Utility));

    for (INamedElement e : elems) {
      //TODO imo what is the \\' suffix for? --> ( of functions, but this may match a substring. query for 'foo' will match the location foobar() !! use non-capturing look-ahead?!
      source = createImportForReferences(e, Pattern.quote(e.getFullyQualifiedName()) + "([^\\']{1})", e.getName() + "$1", source, jsFile, context);
    }
    workingCopy.setSource(source);
  }

  private String createImportForReferences(INamedElement e, String pattern, String replacement, String source, JsFile jsFile, Context context) {
    Matcher matcher = Pattern.compile(pattern).matcher(source);

    boolean result = matcher.find();
    if (result) {
      String filename = jsFile.getPath().getFileName().toString();
      String fqn = e.getFullyQualifiedName();
      StringBuffer sb = new StringBuffer();
      // loop over all because of logging reasons
      do {
        matcher.appendReplacement(sb, replacement);
        LOG.debug("Reference replacement[" + filename + "]: '" + matcher.group() + "' -> '" + replacement + "'");
        result = matcher.find();
      }
      while (result);
      // create import
      LOG.debug("[" + filename + "] Create import for '" + fqn + "'.");
      jsFile.getOrCreateImport(fqn, context);

      matcher.appendTail(sb);
      source = sb.toString();
    }

    return source;
  }

}
