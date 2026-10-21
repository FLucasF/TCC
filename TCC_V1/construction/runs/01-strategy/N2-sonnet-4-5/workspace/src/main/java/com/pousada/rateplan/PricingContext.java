package com.pousada.rateplan;

import com.pousada.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingContext {
    private final RoomType roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final String companyCode;

    public PricingContext(RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                         int guests, String companyCode) {
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.companyCode = companyCode;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public int getNights() {
        return (int) checkIn.until(checkOut, java.time.temporal.ChronoUnit.DAYS);
    }
}
