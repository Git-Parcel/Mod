package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

/** A project-owned message sent from the server to a Git Parcel client. */
public sealed interface ServerMessage
    permits UpdateGitOperationsMessage,
        UpdateParcelFormatsMessage,
        UpdateParcelHistoryMessage,
        UpdateParcelsMessage,
        UpdateSharedRepositoriesMessage {}
