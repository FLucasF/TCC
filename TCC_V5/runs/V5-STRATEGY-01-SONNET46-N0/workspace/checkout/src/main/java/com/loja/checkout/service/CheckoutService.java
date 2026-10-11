package com.loja.checkout.service;

import com.loja.checkout.model.*;
import com.loja.checkout.strategy.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CheckoutService {

    private static final Map<String, ModalidadeEntrega> MODALIDADES = new LinkedHashMap<>();
    private static final Map<String, FormaPagamento> FORMAS_PAGAMENTO = new LinkedHashMap<>();
    private static final Set<String> NIVEIS_CLUBE = Set.of("BRONZE", "PRATA", "OURO");
    private static final Map<String, BigDecimal> TAXAS_SEGURO = Map.of(
            "SUDESTE", BigDecimal.valueOf(0.01),
            "SUL", BigDecimal.valueOf(0.01),
            "CENTRO_OESTE", BigDecimal.valueOf(0.015),
            "NORTE", BigDecimal.valueOf(0.025),
            "NORDESTE", BigDecimal.valueOf(0.02)
    );
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");

    static {
        for (ModalidadeEntrega m : List.of(
                new ModalidadeEconomica(),
                new ModalidadeExpressa(),
                new ModalidadeRetiradaLoja(),
                new ModalidadeMotoboy()
        )) {
            MODALIDADES.put(m.codigo(), m);
        }
        for (FormaPagamento f : List.of(
                new FormaPagamentoPix(),
                new FormaPagamentoBoleto(),
                new FormaPagamentoCartao()
        )) {
            FORMAS_PAGAMENTO.put(f.codigo(), f);
        }
    }

    public Object calcularResumo(PedidoRequest req) {
        // 1. Validar pedido
        if (req.itens() == null || req.itens().isEmpty()) {
            return new ErroResponse("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : req.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg() <= 0) {
                return new ErroResponse("PEDIDO_INVALIDO");
            }
        }

        // 2. Validar nível do clube
        if (req.nivelClube() == null || !NIVEIS_CLUBE.contains(req.nivelClube())) {
            return new ErroResponse("NIVEL_CLUBE_INVALIDO");
        }

        // 3. Validar região
        if (req.regiao() == null || !TAXAS_SEGURO.containsKey(req.regiao())) {
            return new ErroResponse("REGIAO_INVALIDA");
        }

        // 4. Validar modalidade de entrega (existe)
        if (req.modalidadeEntrega() == null || !MODALIDADES.containsKey(req.modalidadeEntrega())) {
            return new ErroResponse("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade = MODALIDADES.get(req.modalidadeEntrega());

        // Calcular peso total
        double pesoTotal = req.itens().stream()
                .mapToDouble(i -> i.pesoKg() * i.quantidade())
                .sum();

        // 5. Verificar se modalidade atende o pedido
        if (!modalidade.aceita(pesoTotal)) {
            return new ErroResponse("MODALIDADE_INDISPONIVEL");
        }

        // 6. Validar cupom (existe)
        String cupom = req.cupom();
        if (cupom != null && !cupom.isBlank() && !CUPONS.contains(cupom)) {
            return new ErroResponse("CUPOM_INVALIDO");
        }

        // 7. Validar forma de pagamento (existe)
        if (req.formaPagamento() == null || !FORMAS_PAGAMENTO.containsKey(req.formaPagamento())) {
            return new ErroResponse("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento = FORMAS_PAGAMENTO.get(req.formaPagamento());

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;

        // 8. Validar parcelas permitidas para forma de pagamento
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            return new ErroResponse("PARCELAMENTO_INVALIDO");
        }

        // --- Calcular subtotal dos produtos ---
        BigDecimal subtotalProdutos = req.itens().stream()
                .map(i -> BigDecimal.valueOf(i.precoUnitario())
                        .multiply(BigDecimal.valueOf(i.quantidade()))
                        .setScale(2, RoundingMode.HALF_EVEN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Calcular desconto do cupom ---
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        boolean freteGratisCupom = false;

        if (cupom != null && !cupom.isBlank()) {
            switch (cupom) {
                case "BEMVINDO10" -> descontoCupom = subtotalProdutos
                        .multiply(BigDecimal.valueOf(0.10))
                        .setScale(2, RoundingMode.HALF_EVEN);
                case "MENOS50" -> {
                    if (subtotalProdutos.compareTo(BigDecimal.valueOf(300.00)) < 0) {
                        return new ErroResponse("CUPOM_NAO_APLICAVEL");
                    }
                    descontoCupom = BigDecimal.valueOf(50.00).setScale(2);
                }
                case "FRETEGRATIS" -> freteGratisCupom = true;
                case "LEVE3PAGUE2" -> descontoCupom = calcularDescontoLeve3Pague2(req.itens());
            }
        }

        // --- Calcular frete ---
        boolean ourofSemFrete = "OURO".equals(req.nivelClube());
        BigDecimal freteCalculado = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete;
        if (ourofSemFrete) {
            frete = BigDecimal.ZERO.setScale(2);
        } else if (freteGratisCupom) {
            // frete aparece normalmente, desconto do cupom = valor do frete
            frete = freteCalculado;
            descontoCupom = freteCalculado;
        } else {
            frete = freteCalculado;
        }

        // --- Calcular seguro ---
        BigDecimal taxaSeguro = TAXAS_SEGURO.get(req.regiao());
        BigDecimal seguro = subtotalProdutos.multiply(taxaSeguro).setScale(2, RoundingMode.HALF_EVEN);

        // --- Total do pedido (antes do ajuste de pagamento) ---
        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        // 9. Verificar disponibilidade da forma de pagamento (ex: boleto acima de 1000)
        if (!formaPagamento.aceita(totalPedido, parcelas)) {
            return new ErroResponse("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // --- Calcular ajuste de pagamento e total final ---
        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = formaPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, parcelas);

        // --- Crédito clube ---
        BigDecimal creditoProximaCompra = BigDecimal.ZERO.setScale(2);
        if ("PRATA".equals(req.nivelClube())) {
            creditoProximaCompra = subtotalProdutos.multiply(BigDecimal.valueOf(0.02))
                    .setScale(2, RoundingMode.HALF_EVEN);
        } else if ("OURO".equals(req.nivelClube())) {
            creditoProximaCompra = subtotalProdutos.multiply(BigDecimal.valueOf(0.05))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        // --- Brinde OURO ---
        boolean brinde = "OURO".equals(req.nivelClube())
                && subtotalProdutos.compareTo(BigDecimal.valueOf(500.00)) > 0;

        return new ResumoResponse(
                subtotalProdutos.doubleValue(),
                descontoCupom.doubleValue(),
                frete.doubleValue(),
                modalidade.prazoEntregaDias(),
                seguro.doubleValue(),
                ajustePagamento.doubleValue(),
                totalFinal.doubleValue(),
                parcelas,
                valorParcela.doubleValue(),
                creditoProximaCompra.doubleValue(),
                brinde
        );
    }

    private BigDecimal calcularDescontoLeve3Pague2(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int qtd = item.quantidade();
            int gratuitas = qtd / 3;
            if (gratuitas > 0) {
                desconto = desconto.add(
                        BigDecimal.valueOf(item.precoUnitario())
                                .multiply(BigDecimal.valueOf(gratuitas))
                                .setScale(2, RoundingMode.HALF_EVEN)
                );
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
