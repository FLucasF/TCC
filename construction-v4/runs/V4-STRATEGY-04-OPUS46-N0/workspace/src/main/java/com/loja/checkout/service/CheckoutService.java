package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class CheckoutService {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    private static final Set<String> NIVEIS_CLUBE = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");
    private static final Set<String> FORMAS_PAGAMENTO = Set.of("PIX", "CARTAO", "BOLETO");

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        String nivelClube = request.nivelClube();
        if (nivelClube == null || !NIVEIS_CLUBE.contains(nivelClube)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        String regiao = request.regiao();
        if (regiao == null || !REGIOES.contains(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        String modalidade = request.modalidadeEntrega();
        if (modalidade == null || !MODALIDADES.contains(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if ("MOTOBOY".equals(modalidade) && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        String cupom = request.cupom();
        if (cupom != null) {
            if (!CUPONS.contains(cupom)) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            if ("MENOS50".equals(cupom) && subtotal.compareTo(new BigDecimal("300")) < 0) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        String formaPagamento = request.formaPagamento();
        if (formaPagamento == null || !FORMAS_PAGAMENTO.contains(formaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        validarParcelas(formaPagamento, parcelas);

        BigDecimal frete = calcularFrete(modalidade, pesoTotal);
        int prazoDias = calcularPrazo(modalidade);

        if ("OURO".equals(nivelClube)) {
            frete = zero();
        }

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotal, frete, request.itens());
        BigDecimal seguro = calcularSeguro(subtotal, regiao);
        BigDecimal totalPedido = round(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if ("BOLETO".equals(formaPagamento) && totalPedido.compareTo(new BigDecimal("1000")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal ajuste;
        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (formaPagamento) {
            case "PIX" -> {
                ajuste = round(totalPedido.multiply(new BigDecimal("0.05"))).negate();
                totalFinal = round(totalPedido.add(ajuste));
                valorParcela = totalFinal;
            }
            case "BOLETO" -> {
                ajuste = new BigDecimal("3.49");
                totalFinal = round(totalPedido.add(ajuste));
                valorParcela = totalFinal;
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    ajuste = zero();
                    totalFinal = totalPedido;
                    valorParcela = round(totalPedido.divide(new BigDecimal(parcelas), SCALE, ROUNDING));
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
                    BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
                    BigDecimal denominador = BigDecimal.ONE.subtract(
                            BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128)
                    );
                    valorParcela = round(totalPedido.multiply(taxa, MathContext.DECIMAL128)
                            .divide(denominador, SCALE, ROUNDING));
                    totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                    ajuste = totalFinal.subtract(totalPedido);
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal creditoRate = switch (nivelClube) {
            case "PRATA" -> new BigDecimal("0.02");
            case "OURO" -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
        BigDecimal credito = round(subtotal.multiply(creditoRate));

        boolean brinde = "OURO".equals(nivelClube)
                && subtotal.compareTo(new BigDecimal("500")) > 0;

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                prazoDias,
                seguro,
                round(ajuste),
                round(totalFinal),
                parcelas,
                valorParcela,
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item == null
                    || item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade()))
            );
        }
        return round(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal calcularFrete(String modalidade, BigDecimal peso) {
        BigDecimal frete = switch (modalidade) {
            case "ECONOMICA" -> new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(peso));
            case "EXPRESSA" -> new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(peso));
            case "RETIRADA_LOJA" -> BigDecimal.ZERO;
            case "MOTOBOY" -> new BigDecimal("18.00");
            default -> throw new CheckoutException("MODALIDADE_INVALIDA");
        };
        return round(frete);
    }

    private int calcularPrazo(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> 0;
        };
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal,
                                              BigDecimal frete, List<ItemRequest> itens) {
        if (cupom == null) {
            return zero();
        }
        return switch (cupom) {
            case "BEMVINDO10" -> round(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50" -> new BigDecimal("50.00");
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> {
                BigDecimal desconto = BigDecimal.ZERO;
                for (ItemRequest item : itens) {
                    int gratis = item.quantidade() / 3;
                    desconto = desconto.add(
                            item.precoUnitario().multiply(new BigDecimal(gratis))
                    );
                }
                yield round(desconto);
            }
            default -> zero();
        };
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, String regiao) {
        BigDecimal taxa = switch (regiao) {
            case "SUDESTE", "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> throw new CheckoutException("REGIAO_INVALIDA");
        };
        return round(subtotal.multiply(taxa));
    }

    private void validarParcelas(String formaPagamento, int parcelas) {
        switch (formaPagamento) {
            case "PIX", "BOLETO" -> {
                if (parcelas != 1) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
            case "CARTAO" -> {
                if (parcelas < 1 || parcelas > 12) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
        }
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }
}
