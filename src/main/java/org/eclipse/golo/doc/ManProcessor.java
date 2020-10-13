/*
 * Copyright (c) 2012-2020 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.doc;

import gololang.FunctionReference;
import gololang.IO;

import java.nio.file.Path;
import java.util.Collection;

public class ManProcessor extends AbstractProcessor {

  @Override
  protected String fileExtension() {
    return "1";
  }

  @Override
  public String render(ModuleDocumentation documentation) throws Throwable {
    FunctionReference template = template("template", fileExtension());
    addModule(documentation);
    return (String) template.invoke(documentation);
  }

  @Override
  public void process(Collection<ModuleDocumentation> modules, Path targetFolder) throws Throwable {
    setTargetFolder(targetFolder);
    for (ModuleDocumentation doc : modules) {
      IO.textToFile(render(doc), outputFile(doc.moduleName()));
    }
  }
}
