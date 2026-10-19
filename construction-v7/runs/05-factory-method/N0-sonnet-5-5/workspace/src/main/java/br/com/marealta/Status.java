package br.com.marealta;

enum Status {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
