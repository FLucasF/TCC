package br.com.pousada.marealta.model;

public record Room(
        int number,
        RoomType type,
        int capacity,
        double nightlyRate
) {
    public static Room from(int number, RoomType type) {
        return new Room(number, type, type.getCapacity(), type.getNightlyRate());
    }
}
