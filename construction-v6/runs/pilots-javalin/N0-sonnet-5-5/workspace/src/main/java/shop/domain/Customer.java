package shop.domain;

import java.math.BigDecimal;

public record Customer(String cpf, BigDecimal creditBalance) {
}
