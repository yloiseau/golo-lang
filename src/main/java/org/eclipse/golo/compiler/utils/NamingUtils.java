/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.utils;

/**
 * This class centralize utility functions to deal with class and function names.
 * <p>
 * Names manipulations such as conversion, mangling and extraction.
 */
public final class NamingUtils {

  public static final char PACKAGE_CLASS_SEPARATOR = '.';
  public static final char INNER_SEPARATOR = '$';

  private NamingUtils() {
    throw new UnsupportedOperationException("don't instantiate utility class");
  }

  public static int packageClassSeparatorIndex(String moduleName) {
    if (moduleName != null) {
      return moduleName.lastIndexOf(PACKAGE_CLASS_SEPARATOR);
    }
    return -1;
  }

  public static String extractTargetJavaPackage(String moduleName) {
    int packageClassSeparatorIndex = packageClassSeparatorIndex(moduleName);
    if (packageClassSeparatorIndex > 0) {
      return moduleName.substring(0, packageClassSeparatorIndex);
    }
    return "";
  }

  public static String extractTargetJavaClass(String moduleName) {
    int packageClassSeparatorIndex = packageClassSeparatorIndex(moduleName);
    if (packageClassSeparatorIndex > 0) {
      return moduleName.substring(packageClassSeparatorIndex + 1);
    }
    return moduleName;
  }

  public static String extractFunctionName(String fullyQualifiedName) {
    return extractTargetJavaClass(fullyQualifiedName);
  }

  public static String extractModuleName(String fullyQualifiedName) {
    return extractTargetJavaPackage(fullyQualifiedName);
  }

  public static boolean isQualified(String name) {
    return packageClassSeparatorIndex(name) > 0;
  }

  public static boolean isInnerClass(String className) {
    if (className == null) {
      return false;
    }
    return className.indexOf(INNER_SEPARATOR) > 0;
  }

  public static String extractContainingClass(String className) {
    if (isInnerClass(className)) {
      return className.substring(0, className.indexOf(INNER_SEPARATOR));
    }
    return "";
  }

  public static boolean isInnerClassOf(String outerName, String innerName) {
    return innerName.startsWith(outerName + INNER_SEPARATOR);
  }

  public static String innerClassOf(String outerName, String innerName) {
    if (isInnerClassOf(outerName, innerName)) {
      return innerName;
    }
    return outerName + INNER_SEPARATOR + mangleName(innerName);
  }

  public static String mangleName(String name) {
    return name.replace(PACKAGE_CLASS_SEPARATOR, INNER_SEPARATOR);
  }

  public static boolean isInModule(String moduleName, String objectName) {
    return objectName.startsWith(moduleName + PACKAGE_CLASS_SEPARATOR);
  }

  public static String inModule(String moduleName, String className) {
    if (isInModule(moduleName, className)) {
      return className;
    }
    return moduleName + PACKAGE_CLASS_SEPARATOR + className;
  }

}
