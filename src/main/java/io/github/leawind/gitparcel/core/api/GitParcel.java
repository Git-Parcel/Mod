package io.github.leawind.gitparcel.core.api;

import io.github.leawind.gitparcel.client.api.GitParcelClient;

/** The main class for the Git Parcel mod. */
public interface GitParcel {

  /** The mod ID for Git Parcel. */
  String MOD_ID = "gitparcel";

  String PROTOCOL_VERSION = "1";

  @Deprecated
  static GitParcelClient client() {
    return Bridge.getGitParcelClient();
  }
}
