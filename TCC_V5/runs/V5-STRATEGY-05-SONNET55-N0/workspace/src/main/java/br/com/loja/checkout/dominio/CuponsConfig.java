package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CuponsConfig {

    private static final BigDecimal MINIMO_MENOS50 = new BigDecimal("300.00");
    private static final BigDecimal VALOR_MENOS50 = new BigDecimal("50.00");

    @Bean
    Cupom bemvindo10() {
        return cupom("BEMVINDO10", c -> true, c -> Dinheiro.percentual(c.subtotal(), BigDecimal.TEN));
    }

    @Bean
    Cupom menos50() {
        return cupom("MENOS50", c -> c.subtotal().compareTo(MINIMO_MENOS50) >= 0, c -> VALOR_MENOS50);
    }

    @Bean
    Cupom freteGratis() {
        return cupom("FRETEGRATIS", c -> true, Cupom.Contexto::frete);
    }

    @Bean
    Cupom leve3Pague2() {
        return cupom("LEVE3PAGUE2", c -> true, c -> c.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_EVEN));
    }

    private static Cupom cupom(String codigo, Predicate<Cupom.Contexto> aplicavel,
                               Function<Cupom.Contexto, BigDecimal> desconto) {
        return new Cupom() {
            public String codigo() {
                return codigo;
            }

            public boolean aplicavel(Contexto contexto) {
                return aplicavel.test(contexto);
            }

            public BigDecimal desconto(Contexto contexto) {
                return Dinheiro.arredondar(desconto.apply(contexto));
            }
        };
    }
}
