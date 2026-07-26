package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

/** A project-owned message sent from a Git Parcel client to the server. */
public sealed interface ClientMessage permits QueryServerStateMessage {}
