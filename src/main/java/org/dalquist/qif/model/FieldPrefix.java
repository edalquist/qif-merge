package org.dalquist.qif.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface FieldPrefix {
  String value();

  boolean printWhenEmpty() default false;

  Class<? extends Block> blockType() default Block.class;
}
