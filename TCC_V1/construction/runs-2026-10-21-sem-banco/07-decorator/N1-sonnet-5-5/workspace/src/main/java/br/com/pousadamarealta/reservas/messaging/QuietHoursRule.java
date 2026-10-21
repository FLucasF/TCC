package br.com.pousadamarealta.reservas.messaging;

import java.time.Clock;
import java.time.LocalTime;

/** Bloqueia mensagens no intervalo de silêncio [from, until), que pode atravessar a meia-noite. */
public final class QuietHoursRule implements MessageRule {

    private final Clock clock;
    private final LocalTime from;
    private final LocalTime until;

    public QuietHoursRule(Clock clock, LocalTime from, LocalTime until) {
        this.clock = clock;
        this.from = from;
        this.until = until;
    }

    @Override
    public void apply(Draft draft) {
        LocalTime now = LocalTime.now(clock);
        boolean quiet = from.isBefore(until)
                ? !now.isBefore(from) && now.isBefore(until)
                : !now.isBefore(from) || now.isBefore(until);
        if (quiet) {
            draft.block();
        }
    }
}
