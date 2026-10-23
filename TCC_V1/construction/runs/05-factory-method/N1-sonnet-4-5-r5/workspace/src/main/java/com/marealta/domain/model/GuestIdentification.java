package com.marealta.domain.model;

public sealed interface GuestIdentification
    permits CpfGuest, CompanyGuest, PassportGuest {

    String getGuestName();
    String getGuestEmail();
}
