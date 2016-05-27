/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;

/**
 * An atomic element of formating.
 */
public final class Element {

    public enum Type {
      KEYWORD("k"),
      VARIABLE("v"),
      NAME("n"),
      OPERATOR("o"),
      COMMENT("c"),
      DOCUMENTATION("ds"),
      ANNOTATION("a"),
      LITTERAL_NUMBER("ln"),
      TYPE("t"),
      LITTERAL_STRING("ls"),
      DELIMITER("d"),
      FORMAT("");

      private final String repr;

      Type(String repr) {
        this.repr = repr;
      }

      public String repr() {
        return repr;
      }
    }

    private final String content;
    private final Type type;

    public Element(Type t, String c) {
      this.content = c;
      this.type = t;
    }

    public String toString() {
      return content;
    }

    public int length() {
      return content.length();
    }

    public static Element keyword(Object v) {
      return new Element(Type.KEYWORD, v.toString());
    }

    public static Element name(Object v) {
      return new Element(Type.NAME, v.toString());
    }

    public static Element space() {
      return new Element(Type.FORMAT, " ");
    }

    public static Element format(Object v) {
      return new Element(Type.FORMAT, v.toString());
    }

    public static Element operator(Object v) {
      return new Element(Type.OPERATOR, v.toString());
    }

    public static Element delimiter(Object v) {
      return new Element(Type.DELIMITER, v.toString());
    }

}
