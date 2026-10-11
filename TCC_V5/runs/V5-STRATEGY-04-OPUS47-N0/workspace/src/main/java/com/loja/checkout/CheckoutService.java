package com.loja.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class CheckoutService {

    private static final BigDecimal ZERO = bd("0.00");
    private static final Set<String> NIVEIS = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");
    private static final Set<String> PAGAMENTOS = Set.of("PIX", "CARTAO", "BOLETO");

    public CheckoutResponse calcular(CheckoutRequest req) {
        List<CheckoutRequest.Item> itens = req.itens();
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (CheckoutRequest.Item i : itens) {
            if (i == null) throw new CheckoutException("PEDIDO_INVALIDO");
            if (i.precoUnitario() == null || i.precoUnitario().signum() <= 0) throw new CheckoutException("PEDIDO_INVALIDO");
            if (i.quantidade() == null || i.quantidade() <= 0) throw new CheckoutException("PEDIDO_INVALIDO");
            if (i.pesoKg() == null || i.pesoKg().signum() <= 0) throw new CheckoutException("PEDIDO_INVALIDO");
        }

        String nivel = req.nivelClube();
        if (nivel == null || !NIVEIS.contains(nivel)) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        String regiao = req.regiao();
        if (regiao == null || !REGIOES.contains(regiao)) throw new CheckoutException("REGIAO_INVALIDA");

        String modalidade = req.modalidadeEntrega();
        if (modalidade == null || !MODALIDADES.contains(modalidade)) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal subtotal = ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        for (CheckoutRequest.Item i : itens) {
            BigDecimal qty = BigDecimal.valueOf(i.quantidade());
            subtotal = subtotal.add(i.precoUnitario().multiply(qty));
            peso = peso.add(i.pesoKg().multiply(qty));
        }
        subtotal = round(subtotal);

        if ("MOTOBOY".equals(modalidade) && peso.compareTo(bd("5")) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        String cupom = req.cupom();
        if (cupom != null && !CUPONS.contains(cupom)) throw new CheckoutException("CUPOM_INVALIDO");
        if ("MENOS50".equals(cupom) && subtotal.compareTo(bd("300")) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        BigDecimal freteOriginal = calcularFrete(modalidade, peso);
        int prazoDias = prazo(modalidade);
        BigDecimal frete = "OURO".equals(nivel) ? ZERO : freteOriginal;

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, subtotal, itens, frete);

        BigDecimal seguro = round(subtotal.multiply(seguroPct(regiao)));

        BigDecimal totalPedido = round(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        String pagamento = req.formaPagamento();
        if (pagamento == null || !PAGAMENTOS.contains(pagamento)) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if ("PIX".equals(pagamento) || "BOLETO".equals(pagamento)) {
            if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        } else {
            if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if ("BOLETO".equals(pagamento) && totalPedido.compareTo(bd("1000")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        PagamentoResultado pag = aplicarPagamento(pagamento, totalPedido, parcelas);

        BigDecimal credito = round(subtotal.multiply(creditoPct(nivel)));
        boolean brinde = "OURO".equals(nivel) && subtotal.compareTo(bd("500")) > 0;

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                prazoDias,
                seguro,
                pag.ajuste(),
                pag.totalFinal(),
                parcelas,
                pag.valorParcela(),
                credito,
                brinde
        );
    }

    private BigDecimal calcularFrete(String modalidade, BigDecimal peso) {
        return switch (modalidade) {
            case "ECONOMICA" -> round(bd("12.00").add(bd("2.00").multiply(peso)));
            case "EXPRESSA" -> round(bd("25.00").add(bd("4.50").multiply(peso)));
            case "RETIRADA_LOJA" -> ZERO;
            case "MOTOBOY" -> bd("18.00");
            default -> throw new IllegalStateException();
        };
    }

    private int prazo(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> throw new IllegalStateException();
        };
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal,
                                             List<CheckoutRequest.Item> itens, BigDecimal frete) {
        if (cupom == null) return ZERO;
        return switch (cupom) {
            case "BEMVINDO10" -> round(subtotal.multiply(bd("0.10")));
            case "MENOS50" -> bd("50.00");
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> {
                BigDecimal total = ZERO;
                for (CheckoutRequest.Item i : itens) {
                    int livres = i.quantidade() / 3;
                    total = total.add(i.precoUnitario().multiply(BigDecimal.valueOf(livres)));
                }
                yield round(total);
            }
            default -> ZERO;
        };
    }

    private BigDecimal seguroPct(String regiao) {
        return switch (regiao) {
            case "SUDESTE", "SUL" -> bd("0.01");
            case "CENTRO_OESTE" -> bd("0.015");
            case "NORTE" -> bd("0.025");
            case "NORDESTE" -> bd("0.02");
            default -> throw new IllegalStateException();
        };
    }

    private BigDecimal creditoPct(String nivel) {
        return switch (nivel) {
            case "BRONZE" -> BigDecimal.ZERO;
            case "PRATA" -> bd("0.02");
            case "OURO" -> bd("0.05");
            default -> throw new IllegalStateException();
        };
    }

    private PagamentoResultado aplicarPagamento(String pagamento, BigDecimal totalPedido, int parcelas) {
        return switch (pagamento) {
            case "PIX" -> {
                BigDecimal desconto = round(totalPedido.multiply(bd("0.05")));
                BigDecimal totalFinal = round(totalPedido.subtract(desconto));
                yield new PagamentoResultado(desconto.negate(), totalFinal, totalFinal);
            }
            case "BOLETO" -> {
                BigDecimal totalFinal = round(totalPedido.add(bd("3.49")));
                yield new PagamentoResultado(bd("3.49"), totalFinal, totalFinal);
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    BigDecimal valorParcela = round(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                    yield new PagamentoResultado(ZERO, totalPedido, valorParcela);
                }
                double i = 0.0199;
                double fator = i / (1.0 - Math.pow(1.0 + i, -parcelas));
                BigDecimal parcela = round(totalPedido.multiply(BigDecimal.valueOf(fator)));
                BigDecimal totalFinal = round(parcela.multiply(BigDecimal.valueOf(parcelas)));
                BigDecimal ajuste = round(totalFinal.subtract(totalPedido));
                yield new PagamentoResultado(ajuste, totalFinal, parcela);
            }
            default -> throw new IllegalStateException();
        };
    }

    private record PagamentoResultado(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
