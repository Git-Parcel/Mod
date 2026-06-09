package io.github.leawind.gitparcel.util.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({
  ElementType.TYPE,
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.CONSTRUCTOR,
  ElementType.PACKAGE,
  ElementType.RECORD_COMPONENT,
})
public @interface VersionSensitive {
  String value() default "";
}
