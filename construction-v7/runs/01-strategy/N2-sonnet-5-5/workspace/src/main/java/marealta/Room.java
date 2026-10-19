package marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Room(int number, RoomType type) {
    public int capacity() { return type.capacity(); }
    public BigDecimal nightlyRate() { return type.nightlyRate(); }

    /** Preço de uma noite, já com o acréscimo de alta temporada. */
    public BigDecimal priceOn(LocalDate night) {
        return Season.isHigh(night) ? Money.percent(nightlyRate(), 120) : nightlyRate();
    }
}
