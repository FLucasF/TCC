package reference;

/**
 * The TransLog quote, already read ("A TransLog", in the prompt's annex). The reference only sees one of
 * these three answers; the real HTTP is spoken by the suite's fake server.
 */
@FunctionalInterface
public interface TransLog {

    /** {@code service} is "STD" (our ECONOMY) or "EXP" (our EXPRESS). */
    Answer quote(String zipCode, int weightGrams, String service);

    sealed interface Answer permits Quote, ZipNotServed, Unavailable {}

    /** {@code 200 { "price_cents": ..., "eta_hours": ... }} */
    record Quote(long priceCents, int etaHours) implements Answer {}

    /** {@code 200 { "error": "ZIP_NOT_SERVED" }} */
    record ZipNotServed() implements Answer {}

    /** Any other answer, or no answer within 2 seconds. */
    record Unavailable() implements Answer {}
}
