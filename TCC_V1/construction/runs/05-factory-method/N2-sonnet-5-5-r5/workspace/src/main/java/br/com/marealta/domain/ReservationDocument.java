package br.com.marealta.domain;

import java.util.ArrayList;
import java.util.List;

/** Esqueleto de todo documento da reserva; cada tipo muda só os passos que são dele. */
public abstract class ReservationDocument {
    protected final Reservation reservation;

    protected ReservationDocument(Reservation reservation) {
        this.reservation = reservation;
    }

    public abstract String title();

    protected abstract String heading();

    protected abstract List<String> guestLines();

    protected abstract String roomLine();

    protected abstract String periodLine();

    protected abstract String subtotalLine();

    protected abstract String discountLine();

    protected abstract String totalLine();

    protected List<String> closingLines() {
        return List.of();
    }

    public final Reservation reservation() {
        return reservation;
    }

    public final String text() {
        List<String> lines = new ArrayList<>();
        lines.add(heading());
        lines.addAll(guestLines());
        lines.add(roomLine());
        lines.add(periodLine());
        lines.add(subtotalLine());
        if (reservation.discount().signum() > 0) {
            lines.add(discountLine());
        }
        lines.add(totalLine());
        lines.addAll(closingLines());
        return String.join("\n", lines);
    }
}
