package com.marealta.service;

import com.marealta.domain.ErrorCode;
import com.marealta.domain.HotelException;
import com.marealta.domain.RatePlan;

import java.time.LocalDate;

public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    public NewReservation {
        if (!checkOut.isAfter(checkIn)) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
    }
}
