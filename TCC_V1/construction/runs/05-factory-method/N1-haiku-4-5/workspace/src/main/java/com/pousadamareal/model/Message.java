package com.pousadamareal.model;

import com.google.gson.annotations.SerializedName;

public record Message(
    @SerializedName("id")
    int id,
    @SerializedName("channel")
    String channel,
    @SerializedName("to")
    String to,
    @SerializedName("subject")
    String subject,
    @SerializedName("body")
    String body
) {}
