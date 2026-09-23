package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebService.WebResponse;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;

/** Handlers for the shared repository endpoints. */
final class RepositoryEndpoints {
  private RepositoryEndpoints() {}

  static WebResponse list(WebApi api) {
    var repositories =
        api.onServerThread(
            () -> {
              var array = new JsonArray();
              for (var entry : SharedRepositoryService.get(api.server()).list().entrySet()) {
                array.add(ParcelJson.repository(entry.getKey(), entry.getValue()));
              }
              return array;
            });
    var body = new JsonObject();
    body.add("repositories", repositories);
    return WebResponse.json(200, ApiJson.GSON.toJson(body));
  }

  static WebResponse create(WebApi api, JsonObject request) {
    var name = ApiJson.requireString(request, "name");
    return api.submitOperation(
        "create",
        name,
        progress -> {
          SharedRepositoryService.get(api.server()).create(name);
          return "Created";
        });
  }

  static WebResponse cloneRepository(WebApi api, String name, JsonObject request) {
    var remoteUrl = ApiJson.requireString(request, "url");
    return api.submitOperation(
        "clone",
        name,
        progress -> {
          SharedRepositoryService.get(api.server()).cloneRepository(name, remoteUrl);
          return "Cloned";
        });
  }

  static WebResponse fetch(WebApi api, String name) {
    return api.submitOperation(
        "fetch",
        name,
        progress ->
            SharedRepositoryService.get(api.server()).fetch(name)
                + " tracking reference(s) updated");
  }

  static WebResponse pull(WebApi api, String name) {
    return api.submitOperation(
        "pull", name, progress -> SharedRepositoryService.get(api.server()).pull(name));
  }

  static WebResponse push(WebApi api, String name) {
    return api.submitOperation(
        "push",
        name,
        progress ->
            SharedRepositoryService.get(api.server()).push(name)
                + " remote reference(s) updated");
  }
}
