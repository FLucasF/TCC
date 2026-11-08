package com.pousadamarealta.domain;

public enum RoomType {
  STANDARD("STANDARD"),
  DELUXE("DELUXE"),
  SUITE("SUITE");

  public final String value;

  RoomType(String value) {
    this.value = value;
  }

  public static RoomType from(String value) {
    for (RoomType type : RoomType.values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown room type: " + value);
  }
}
