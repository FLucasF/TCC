package com.pousadamarealta.domain;

public record Room(
    int number,
    RoomType type,
    int capacity,
    double nightlyRate
) {}
