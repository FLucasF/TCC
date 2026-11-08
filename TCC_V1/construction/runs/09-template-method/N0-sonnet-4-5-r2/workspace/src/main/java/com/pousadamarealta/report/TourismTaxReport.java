package com.pousadamarealta.report;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.List;

public class TourismTaxReport extends BaseReport {
    private final ReservationRepository reservationRepository;
    private final PricingService pricingService;

    public TourismTaxReport() {
        this.reservationRepository = new ReservationRepository();
        this.pricingService = new PricingService();
    }

    public String generate(int year, int month) {
        addTitleLine("tourism-tax", String.format("%04d-%02d", year, month));
        addHeaderLine("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");

        List<Reservation> reservations = reservationRepository.findByMonth(year, month);
        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            if (r.getStatus() == ReservationStatus.CONFIRMED ||
                r.getStatus() == ReservationStatus.CHECKED_IN ||
                r.getStatus() == ReservationStatus.CHECKED_OUT) {

                BigDecimal tax = pricingService.calculateTourismTax(r.getGuests(), r.getNights());

                addDataLine(
                    r.getId().toString(),
                    r.getRoomNumber(),
                    DateUtils.formatISO(r.getCheckIn()),
                    DateUtils.formatISO(r.getCheckOut()),
                    String.valueOf(r.getGuests()),
                    String.valueOf(r.getNights()),
                    MoneyUtils.formatCSV(tax)
                );

                totalTax = totalTax.add(tax);
            }
        }

        addDataLine("total", MoneyUtils.formatCSV(totalTax));

        return super.generate();
    }
}
