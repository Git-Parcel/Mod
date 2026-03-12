package io.github.leawind.gitparcel.permission;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;

public interface ReadablePermissionSettings {

  byte get(PermissionType type);

  long toLong(int level);

  Object2ByteMap<String> toMap();
}
