package com.loja.checkout.service;

import com.loja.checkout.cupom.CalculoCupom;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.entrega.CalculoEntrega;
import com.loja.checkout.entrega.ResultadoEntrega;
import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private static final Set<String> NIVEIS_CLUBE = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> FORMAS_PAGAMENTO = Set.of("PIX", "CARTAO", "BOLETO");

    private static final Map<String, BigDecimal> TAXAS_SEGURO = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02"));

    private final Map<String, CalculoEntrega> entregas;
    private final Map<String, CalculoCupom> cupons;

    public CheckoutService(List<CalculoEntrega> entregaList, List<CalculoCupom> cupomList) {
        this.entregas = entregaList.stream()
                .collect(Collectors.toMap(CalculoEntrega::codigo, Function.identity()));
        this.cupons = cupomList.stream()
                .collect(Collectors.toMap(CalculoCupom::codigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());
        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPeso(request.itens());

        String nivelClube = validarNivelClube(request.nivelClube());
        String regiao = validarRegiao(request.regiao());

        CalculoEntrega entrega = validarModalidade(request.modalidadeEntrega());
        entrega.validar(pesoTotal);
        ResultadoEntrega resultadoEntrega = entrega.calcular(pesoTotal);

        BigDecimal frete = resultadoEntrega.frete();
        int prazoDias = resultadoEntrega.prazoDias();

        if ("OURO".equals(nivelClube)) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null && !request.cupom().isBlank()) {
            CalculoCupom cupom = validarCupom(request.cupom());
            cupom.validar(request.itens(), subtotal);
            descontoCupom = cupom.calcularDesconto(request.itens(), subtotal, frete);
        }

        BigDecimal seguro = subtotal.multiply(TAXAS_SEGURO.get(regiao))
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        String formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        validarParcelas(formaPagamento, parcelas);
        validarDisponibilidadePagamento(formaPagamento, totalPedido);

        BigDecimal totalFinal;
        BigDecimal valorParcela;
        BigDecimal ajuste;

        switch (formaPagamento) {
            case "PIX" -> {
                BigDecimal descPix = totalPedido.multiply(new BigDecimal("0.05"))
                        .setScale(2, RoundingMode.HALF_EVEN);
                totalFinal = totalPedido.subtract(descPix);
                ajuste = descPix.negate();
                valorParcela = totalFinal;
            }
            case "BOLETO" -> {
                BigDecimal tarifa = new BigDecimal("3.49");
                totalFinal = totalPedido.add(tarifa);
                ajuste = tarifa;
                valorParcela = totalFinal;
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    totalFinal = totalPedido;
                    valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                    ajuste = BigDecimal.ZERO.setScale(2);
                } else {
                    BigDecimal taxa = new BigDecimal("0.0199");
                    BigDecimal base = BigDecimal.ONE.add(taxa);
                    BigDecimal potencia = BigDecimal.ONE;
                    for (int i = 0; i < parcelas; i++) {
                        potencia = potencia.multiply(base, MathContext.DECIMAL128);
                    }
                    BigDecimal denominador = BigDecimal.ONE.subtract(
                            BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128));
                    valorParcela = totalPedido.multiply(taxa, MathContext.DECIMAL128)
                            .divide(denominador, 2, RoundingMode.HALF_EVEN);
                    totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                    ajuste = totalFinal.subtract(totalPedido);
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal credito = BigDecimal.ZERO.setScale(2);
        boolean brinde = false;

        switch (nivelClube) {
            case "PRATA" -> credito = subtotal.multiply(new BigDecimal("0.02"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            case "OURO" -> {
                credito = subtotal.multiply(new BigDecimal("0.05"))
                        .setScale(2, RoundingMode.HALF_EVEN);
                brinde = subtotal.compareTo(new BigDecimal("500")) > 0;
            }
        }

        return new CheckoutResponse(
                subtotal, descontoCupom, frete, prazoDias, seguro,
                ajuste, totalFinal, parcelas, valorParcela, credito, brinde);
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

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private String validarNivelClube(String nivelClube) {
        if (nivelClube == null || !NIVEIS_CLUBE.contains(nivelClube)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return nivelClube;
    }

    private String validarRegiao(String regiao) {
        if (regiao == null || !TAXAS_SEGURO.containsKey(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        return regiao;
    }

    private CalculoEntrega validarModalidade(String modalidade) {
        if (modalidade == null || !entregas.containsKey(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return entregas.get(modalidade);
    }

    private CalculoCupom validarCupom(String cupom) {
        if (!cupons.containsKey(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return cupons.get(cupom);
    }

    private String validarFormaPagamento(String forma) {
        if (forma == null || !FORMAS_PAGAMENTO.contains(forma)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return forma;
    }

    private void validarParcelas(String forma, int parcelas) {
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

    private void validarDisponibilidadePagamento(String forma, BigDecimal totalPedido) {
        if ("BOLETO".equals(forma) && totalPedido.compareTo(new BigDecimal("1000")) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
