package com.pousadamareal.model;

import com.google.gson.annotations.SerializedName;

public record Room(
    @SerializedName("number")
    int number,
    @SerializedName("type")
    String type,
    @SerializedName("capacity")
    int capacity,
    @SerializedName("nightlyRate")
    double nightlyRate
) {}
