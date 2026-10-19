package br.com.mare;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Planos de tarifa existentes e o registro por código. */
public final class RatePlans {
    private static final Map<String, RatePlan> REGISTRY = new LinkedHashMap<>();

    static {
        register(new Discounted("STANDARD", "0", 1, p -> p));
        register(new Discounted("NON_REFUNDABLE", "0.15", 1, p -> Money.ZERO));
        register(new Corporate());
        register(new WeekendPackage());
        register(new Discounted("LONG_STAY", "0.10", 7, p -> p));
    }

    private RatePlans() {}

    private static void register(RatePlan plan) { REGISTRY.put(plan.code(), plan); }

    public static RatePlan find(String code) { return REGISTRY.get(code); }

    // ---- preço base ----

    public static boolean isHighSeason(LocalDate night) {
        Month m = night.getMonth();
        if (m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY) return true;
        return m == Month.DECEMBER && night.getDayOfMonth() >= 15;
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.fraction(rate, "1.20") : Money.round(rate);
    }

    public static BigDecimal nightsSum(Room room, LocalDate in, LocalDate out) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = in; d.isBefore(out); d = d.plusDays(1)) {
            sum = sum.add(nightPrice(room, d));
        }
        return Money.round(sum);
    }

    // ---- planos ----

    /** Diárias normais com um percentual de desconto, mínimo de noites e regra de devolução. */
    static class Discounted implements RatePlan {
        private final String code;
        private final String discountFraction;
        private final int minNights;
        private final UnaryOperator<BigDecimal> refundRule;

        Discounted(String code, String discountFraction, int minNights, UnaryOperator<BigDecimal> refundRule) {
            this.code = code;
            this.discountFraction = discountFraction;
            this.minNights = minNights;
            this.refundRule = refundRule;
        }

        public String code() { return code; }

        public Quote quote(Room room, LocalDate in, LocalDate out, String companyCode) {
            long nights = ChronoUnit.DAYS.between(in, out);
            if (nights < minNights) {
                throw ApiException.invalid("O plano " + code + " exige no mínimo " + minNights + " noites");
            }
            BigDecimal subtotal = nightsSum(room, in, out);
            return new Quote(subtotal, Money.fraction(subtotal, discountFraction));
        }

        public BigDecimal refund(BigDecimal paid) { return Money.round(refundRule.apply(paid)); }
    }

    /** Convênio com empresas: desconto depende do código da empresa. */
    static class Corporate implements RatePlan {
        private static final Map<String, String> COMPANIES = Map.of(
                "TECNOSUL", "0.12",
                "AGROVALE", "0.18",
                "FARMAVIDA", "0.25");

        public String code() { return "CORPORATE"; }

        public Quote quote(Room room, LocalDate in, LocalDate out, String companyCode) {
            String fraction = companyCode == null ? null : COMPANIES.get(companyCode.trim());
            if (fraction == null) {
                throw ApiException.invalid("Código de empresa ausente ou desconhecido para o plano CORPORATE");
            }
            BigDecimal subtotal = nightsSum(room, in, out);
            return new Quote(subtotal, Money.fraction(subtotal, fraction));
        }

        public BigDecimal refund(BigDecimal paid) { return Money.round(paid); }
    }

    /** Sexta a domingo, duas noites, preço fechado por tipo de quarto; devolve metade. */
    static class WeekendPackage implements RatePlan {
        private static final Map<Room.RoomType, String> PRICES = Map.of(
                Room.RoomType.STANDARD, "520.00",
                Room.RoomType.DELUXE, "820.00",
                Room.RoomType.SUITE, "1200.00");

        public String code() { return "WEEKEND_PACKAGE"; }

        public Quote quote(Room room, LocalDate in, LocalDate out, String companyCode) {
            if (in.getDayOfWeek() != DayOfWeek.FRIDAY || !out.equals(in.plusDays(2))) {
                throw ApiException.invalid("O pacote de fim de semana exige entrada na sexta e saída no domingo seguinte");
            }
            return new Quote(Money.of(PRICES.get(room.type())), Money.ZERO);
        }

        public BigDecimal refund(BigDecimal paid) { return Money.fraction(paid, "0.5"); }
    }
}
