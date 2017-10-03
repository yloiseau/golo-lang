/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package gololang;

import org.testng.annotations.Test;
import org.eclipse.golo.internal.testing.GoloTest;

public class GeneratorTest extends GoloTest {

  @Override
  public String srcDir() {
    return "for-test/";
  }

  @Test
  public void testGenerators() throws Throwable {
    run("generators");
  }

}
