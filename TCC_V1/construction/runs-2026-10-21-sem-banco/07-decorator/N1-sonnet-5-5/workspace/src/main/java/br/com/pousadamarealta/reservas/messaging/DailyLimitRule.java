package br.com.pousadamarealta.reservas.messaging;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Limita quantas mensagens um mesmo hóspede (pelo endereço) recebe por dia. */
public final class DailyLimitRule implements MessageRule {

    private record Key(String address, LocalDate day) {
    }

    private final Clock clock;
    private final int maxPerDay;
    private final Map<Key, Integer> counts = new HashMap<>();

    public DailyLimitRule(Clock clock, int maxPerDay) {
        this.clock = clock;
        this.maxPerDay = maxPerDay;
    }

    @Override
    public synchronized void apply(Draft draft) {
        Key key = new Key(draft.to(), LocalDate.now(clock));
        int sent = counts.getOrDefault(key, 0);
        if (sent >= maxPerDay) {
            draft.block();
            return;
        }
        counts.put(key, sent + 1);
    }
}
