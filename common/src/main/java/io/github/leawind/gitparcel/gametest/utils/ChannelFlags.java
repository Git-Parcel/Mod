package io.github.leawind.gitparcel.gametest.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.LOCAL_VARIABLE,
  ElementType.METHOD,
  ElementType.TYPE_USE
})
public @interface ChannelFlags {
  int BLOCK_STATE = 1;
  int BLOCK_ENTITIY = 2;
  int ENTITIY = 4;

  int BLOCKS = BLOCK_STATE | BLOCK_ENTITIY;
  int ALL = BLOCK_STATE | BLOCK_ENTITIY | ENTITIY;
}
