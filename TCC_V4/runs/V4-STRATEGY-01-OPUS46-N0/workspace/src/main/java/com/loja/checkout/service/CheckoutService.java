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
import java.util.Map;
import java.util.Set;

@Service
public class CheckoutService {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final MathContext MC = new MathContext(20, ROUNDING);

    private static final Set<String> MODALIDADES = Set.of(
            "ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");

    private static final Set<String> NIVEIS_CLUBE = Set.of("BRONZE", "PRATA", "OURO");

    private static final Map<String, BigDecimal> TAXAS_SEGURO = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02"));

    private static final Set<String> CUPONS = Set.of(
            "BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");

    private static final Set<String> FORMAS_PAGAMENTO = Set.of("PIX", "CARTAO", "BOLETO");

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal PESO_MAXIMO_MOTOBOY = new BigDecimal("5");

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());
        validarNivelClube(request.nivelClube());
        validarRegiao(request.regiao());
        validarModalidade(request.modalidadeEntrega());

        List<ItemRequest> itens = request.itens();
        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        validarModalidadeDisponivel(request.modalidadeEntrega(), pesoTotal);
        validarCupom(request.cupom());
        validarCupomAplicavel(request.cupom(), subtotal, itens);
        validarFormaPagamento(request.formaPagamento());

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        validarParcelamento(request.formaPagamento(), parcelas);

        BigDecimal frete = calcularFrete(request.modalidadeEntrega(), pesoTotal);
        int prazo = calcularPrazo(request.modalidadeEntrega());

        boolean ouro = "OURO".equals(request.nivelClube());
        if (ouro) {
            frete = round(BigDecimal.ZERO);
        }

        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), subtotal, frete, itens);
        BigDecimal seguro = round(subtotal.multiply(TAXAS_SEGURO.get(request.regiao()), MC));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        validarFormaPagamentoDisponivel(request.formaPagamento(), totalPedido);

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (request.formaPagamento()) {
            case "PIX" -> {
                BigDecimal desconto = round(totalPedido.multiply(new BigDecimal("0.05"), MC));
                totalFinal = totalPedido.subtract(desconto);
                valorParcela = totalFinal;
            }
            case "BOLETO" -> {
                totalFinal = totalPedido.add(TARIFA_BOLETO);
                valorParcela = totalFinal;
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    totalFinal = totalPedido;
                    valorParcela = round(totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
                } else {
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
                    BigDecimal potencia = umMaisTaxa.pow(parcelas, MC);
                    BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, MC));
                    valorParcela = round(totalPedido.multiply(TAXA_JUROS, MC).divide(fator, MC));
                    totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        BigDecimal credito = calcularCredito(request.nivelClube(), subtotal);
        boolean brinde = ouro && subtotal.compareTo(new BigDecimal("500")) > 0;

        return new CheckoutResponse(
                subtotal, descontoCupom, frete, prazo, seguro,
                round(ajuste), round(totalFinal),
                parcelas, round(valorParcela), credito, brinde);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return round(total);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal calcularFrete(String modalidade, BigDecimal peso) {
        return switch (modalidade) {
            case "ECONOMICA" -> round(new BigDecimal("12").add(new BigDecimal("2").multiply(peso)));
            case "EXPRESSA" -> round(new BigDecimal("25").add(new BigDecimal("4.5").multiply(peso)));
            case "RETIRADA_LOJA" -> round(BigDecimal.ZERO);
            case "MOTOBOY" -> round(new BigDecimal("18"));
            default -> throw new CheckoutException("MODALIDADE_INVALIDA");
        };
    }

    private int calcularPrazo(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> 7;
            case "EXPRESSA" -> 2;
            case "RETIRADA_LOJA" -> 1;
            case "MOTOBOY" -> 0;
            default -> throw new CheckoutException("MODALIDADE_INVALIDA");
        };
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, BigDecimal frete,
                                             List<ItemRequest> itens) {
        if (cupom == null || cupom.isBlank()) {
            return round(BigDecimal.ZERO);
        }
        return switch (cupom) {
            case "BEMVINDO10" -> round(subtotal.multiply(new BigDecimal("0.10"), MC));
            case "MENOS50" -> round(new BigDecimal("50"));
            case "FRETEGRATIS" -> frete;
            case "LEVE3PAGUE2" -> {
                BigDecimal desconto = BigDecimal.ZERO;
                for (ItemRequest item : itens) {
                    int gratis = item.quantidade() / 3;
                    desconto = desconto.add(
                            item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                }
                yield round(desconto);
            }
            default -> throw new CheckoutException("CUPOM_INVALIDO");
        };
    }

    private BigDecimal calcularCredito(String nivel, BigDecimal subtotal) {
        return switch (nivel) {
            case "PRATA" -> round(subtotal.multiply(new BigDecimal("0.02"), MC));
            case "OURO" -> round(subtotal.multiply(new BigDecimal("0.05"), MC));
            default -> round(BigDecimal.ZERO);
        };
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarNivelClube(String nivel) {
        if (nivel == null || !NIVEIS_CLUBE.contains(nivel)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private void validarRegiao(String regiao) {
        if (regiao == null || !TAXAS_SEGURO.containsKey(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private void validarModalidade(String modalidade) {
        if (modalidade == null || !MODALIDADES.contains(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(String modalidade, BigDecimal peso) {
        if ("MOTOBOY".equals(modalidade) && peso.compareTo(PESO_MAXIMO_MOTOBOY) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarCupom(String cupom) {
        if (cupom != null && !cupom.isBlank() && !CUPONS.contains(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private void validarCupomAplicavel(String cupom, BigDecimal subtotal, List<ItemRequest> itens) {
        if (cupom == null || cupom.isBlank()) {
            return;
        }
        if ("MENOS50".equals(cupom) && subtotal.compareTo(new BigDecimal("300")) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private void validarFormaPagamento(String forma) {
        if (forma == null || !FORMAS_PAGAMENTO.contains(forma)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarParcelamento(String forma, int parcelas) {
        switch (forma) {
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

    private void validarFormaPagamentoDisponivel(String forma, BigDecimal totalPedido) {
        if ("BOLETO".equals(forma) && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
