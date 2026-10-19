package com.pousada.domain;

public final class CpfIdentification implements GuestIdentification {
    public String guestCpf;

    public CpfIdentification() {}
    public CpfIdentification(String guestCpf) {
        this.guestCpf = guestCpf;
    }
}
