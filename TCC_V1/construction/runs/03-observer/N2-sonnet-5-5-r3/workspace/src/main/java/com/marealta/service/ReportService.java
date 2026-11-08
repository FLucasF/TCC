package com.marealta.service;

import com.marealta.domain.Money;
import com.marealta.domain.Reservation;
import com.marealta.domain.RoomCatalog;
import com.marealta.persistence.Database;
import com.marealta.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class ReportService {
    private final Database db;
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;

    public ReportService(Database db, ReservationRepository reservations, RoomCatalog rooms) {
        this.db = db;
        this.reservations = reservations;
        this.rooms = rooms;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.transaction(() -> reservations.findPaidOccupying(night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(String.join(",", String.valueOf(r.roomNumber()), rooms.require(r.roomNumber()).type().name(),
                    String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString())).append('\n');
        }
        // um quarto só pode ter uma reserva por noite, então cada linha é um quarto ocupado
        return csv.append("total,").append(occupying.size()).append('/').append(rooms.size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = db.transaction(() ->
                reservations.findRetainingMoney(month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            csv.append(String.join(",", String.valueOf(r.id()), String.valueOf(r.roomNumber()),
                    r.checkIn().toString(), r.checkOut().toString(), r.status().name(),
                    Money.plain(r.retained()))).append('\n');
            total = total.add(r.retained());
        }
        return csv.append("total,").append(Money.plain(total)).append('\n').toString();
    }
}
