/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;

import java.util.stream.Stream;
import java.util.List;

interface FormatingElement {
  int length();
  boolean isEmpty();
  FormatingElement append(Object o);
  Stream<String> split(int width);
  void reset();
  char lastChar();
  List<FormatingElement> children();
}
