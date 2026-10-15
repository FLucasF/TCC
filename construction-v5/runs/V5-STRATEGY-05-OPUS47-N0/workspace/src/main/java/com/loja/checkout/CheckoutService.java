package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final RoundingMode HE = RoundingMode.HALF_EVEN;

    private static final Set<String> NIVEIS = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> FORMAS = Set.of("PIX", "CARTAO", "BOLETO");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");

    private static final BigDecimal LIMITE_MOTOBOY = new BigDecimal("5");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");
    private static final BigDecimal MIN_MENOS50 = new BigDecimal("300");
    private static final BigDecimal MIN_BRINDE = new BigDecimal("500");
    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESC_PIX = new BigDecimal("0.05");

    public ResponseEntity<Object> calcular(PedidoRequest req) {
        if (req == null) return erro("PEDIDO_INVALIDO");

        if (req.itens() == null || req.itens().isEmpty()) return erro("PEDIDO_INVALIDO");
        for (var i : req.itens()) {
            if (i == null) return erro("PEDIDO_INVALIDO");
            if (i.precoUnitario() == null || i.precoUnitario().signum() <= 0) return erro("PEDIDO_INVALIDO");
            if (i.quantidade() == null || i.quantidade() <= 0) return erro("PEDIDO_INVALIDO");
            if (i.pesoKg() == null || i.pesoKg().signum() <= 0) return erro("PEDIDO_INVALIDO");
        }

        if (req.nivelClube() == null || !NIVEIS.contains(req.nivelClube())) return erro("NIVEL_CLUBE_INVALIDO");
        if (req.regiao() == null || !REGIOES.contains(req.regiao())) return erro("REGIAO_INVALIDA");
        if (req.modalidadeEntrega() == null || !MODALIDADES.contains(req.modalidadeEntrega()))
            return erro("MODALIDADE_INVALIDA");

        BigDecimal subtotalBruto = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (var i : req.itens()) {
            BigDecimal qty = new BigDecimal(i.quantidade());
            subtotalBruto = subtotalBruto.add(i.precoUnitario().multiply(qty));
            pesoTotal = pesoTotal.add(i.pesoKg().multiply(qty));
        }
        BigDecimal subtotal = subtotalBruto.setScale(2, HE);

        if ("MOTOBOY".equals(req.modalidadeEntrega()) && pesoTotal.compareTo(LIMITE_MOTOBOY) > 0)
            return erro("MODALIDADE_INDISPONIVEL");

        String cupom = (req.cupom() == null || req.cupom().isEmpty()) ? null : req.cupom();
        if (cupom != null && !CUPONS.contains(cupom)) return erro("CUPOM_INVALIDO");
        if ("MENOS50".equals(cupom) && subtotal.compareTo(MIN_MENOS50) < 0) return erro("CUPOM_NAO_APLICAVEL");

        if (req.formaPagamento() == null || !FORMAS.contains(req.formaPagamento()))
            return erro("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        switch (req.formaPagamento()) {
            case "PIX", "BOLETO" -> {
                if (parcelas != 1) return erro("PARCELAMENTO_INVALIDO");
            }
            case "CARTAO" -> {
                if (parcelas < 1 || parcelas > 12) return erro("PARCELAMENTO_INVALIDO");
            }
        }

        BigDecimal fretePre;
        int prazo;
        switch (req.modalidadeEntrega()) {
            case "ECONOMICA" -> {
                fretePre = new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotal));
                prazo = 7;
            }
            case "EXPRESSA" -> {
                fretePre = new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotal));
                prazo = 2;
            }
            case "RETIRADA_LOJA" -> {
                fretePre = BigDecimal.ZERO;
                prazo = 1;
            }
            case "MOTOBOY" -> {
                fretePre = new BigDecimal("18.00");
                prazo = 0;
            }
            default -> throw new IllegalStateException();
        }
        if ("OURO".equals(req.nivelClube())) fretePre = BigDecimal.ZERO;
        BigDecimal frete = fretePre.setScale(2, HE);

        BigDecimal desconto = BigDecimal.ZERO;
        if (cupom != null) {
            switch (cupom) {
                case "BEMVINDO10" -> desconto = subtotal.multiply(new BigDecimal("0.10"));
                case "MENOS50" -> desconto = new BigDecimal("50.00");
                case "FRETEGRATIS" -> desconto = frete;
                case "LEVE3PAGUE2" -> {
                    BigDecimal d = BigDecimal.ZERO;
                    for (var i : req.itens()) {
                        int free = i.quantidade() / 3;
                        if (free > 0) d = d.add(i.precoUnitario().multiply(new BigDecimal(free)));
                    }
                    desconto = d;
                }
            }
        }
        BigDecimal descontoCupom = desconto.setScale(2, HE);

        BigDecimal segRate = switch (req.regiao()) {
            case "SUDESTE", "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> throw new IllegalStateException();
        };
        BigDecimal seguro = subtotal.multiply(segRate).setScale(2, HE);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro).setScale(2, HE);

        if ("BOLETO".equals(req.formaPagamento()) && totalPedido.compareTo(LIMITE_BOLETO) > 0)
            return erro("FORMA_PAGAMENTO_INDISPONIVEL");

        BigDecimal ajuste;
        BigDecimal totalFinal;
        BigDecimal valorParcela;
        switch (req.formaPagamento()) {
            case "PIX" -> {
                BigDecimal desc = totalPedido.multiply(DESC_PIX).setScale(2, HE);
                ajuste = desc.negate();
                totalFinal = totalPedido.add(ajuste).setScale(2, HE);
                valorParcela = totalFinal;
            }
            case "BOLETO" -> {
                ajuste = TARIFA_BOLETO;
                totalFinal = totalPedido.add(ajuste).setScale(2, HE);
                valorParcela = totalFinal;
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    ajuste = BigDecimal.ZERO.setScale(2);
                    totalFinal = totalPedido;
                    valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, HE);
                } else {
                    BigDecimal onePlusR = BigDecimal.ONE.add(TAXA_CARTAO);
                    BigDecimal pow = onePlusR.pow(parcelas, MC);
                    BigDecimal denom = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(pow, MC), MC);
                    BigDecimal parcelaBruta = totalPedido.multiply(TAXA_CARTAO, MC).divide(denom, MC);
                    valorParcela = parcelaBruta.setScale(2, HE);
                    totalFinal = valorParcela.multiply(new BigDecimal(parcelas)).setScale(2, HE);
                    ajuste = totalFinal.subtract(totalPedido).setScale(2, HE);
                }
            }
            default -> throw new IllegalStateException();
        }

        BigDecimal credRate = switch (req.nivelClube()) {
            case "PRATA" -> new BigDecimal("0.02");
            case "OURO" -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
        BigDecimal credito = subtotal.multiply(credRate).setScale(2, HE);

        boolean brinde = "OURO".equals(req.nivelClube()) && subtotal.compareTo(MIN_BRINDE) > 0;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("subtotalProdutos", subtotal);
        resp.put("descontoCupom", descontoCupom);
        resp.put("frete", frete);
        resp.put("prazoEntregaDias", prazo);
        resp.put("seguro", seguro);
        resp.put("ajustePagamento", ajuste.setScale(2, HE));
        resp.put("totalFinal", totalFinal);
        resp.put("parcelas", parcelas);
        resp.put("valorParcela", valorParcela);
        resp.put("creditoProximaCompra", credito);
        resp.put("brinde", brinde);
        return ResponseEntity.ok(resp);
    }

    private ResponseEntity<Object> erro(String code) {
        return ResponseEntity.ok(Map.of("erro", code));
    }
}
