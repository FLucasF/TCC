package com.loja.checkout.service;

import com.loja.checkout.web.CheckoutException;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

@Service
public class CheckoutService {

    private static final BigDecimal LIMITE_MOTOBOY = new BigDecimal("5");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal MIN_MENOS50 = new BigDecimal("300.00");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");

    private static final Set<String> NIVEIS = Set.of("BRONZE", "PRATA", "OURO");
    private static final Set<String> REGIOES = Set.of("SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");
    private static final Set<String> MODALIDADES = Set.of("ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY");
    private static final Set<String> FORMAS = Set.of("PIX", "CARTAO", "BOLETO");
    private static final Set<String> CUPONS = Set.of("BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2");

    private static final Map<String, BigDecimal> SEGURO_REGIAO = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private static final Map<String, BigDecimal> CREDITO_NIVEL = Map.of(
            "BRONZE", BigDecimal.ZERO,
            "PRATA", new BigDecimal("0.02"),
            "OURO", new BigDecimal("0.05")
    );

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Validar itens e calcular subtotal/peso
        if (req.itens == null || req.itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (CheckoutRequest.Item item : req.itens) {
            if (item == null
                    || item.precoUnitario == null || item.precoUnitario.signum() <= 0
                    || item.quantidade == null || item.quantidade <= 0
                    || item.pesoKg == null || item.pesoKg.signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            BigDecimal qtd = BigDecimal.valueOf(item.quantidade);
            subtotal = subtotal.add(item.precoUnitario.multiply(qtd));
            pesoTotal = pesoTotal.add(item.pesoKg.multiply(qtd));
        }
        subtotal = round(subtotal);

        // 2. Nivel
        if (req.nivelClube == null || !NIVEIS.contains(req.nivelClube)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        String nivel = req.nivelClube;

        // 3. Regiao
        if (req.regiao == null || !REGIOES.contains(req.regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        String regiao = req.regiao;

        // 4. Modalidade
        if (req.modalidadeEntrega == null || !MODALIDADES.contains(req.modalidadeEntrega)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        String modalidade = req.modalidadeEntrega;

        // 5. Modalidade disponivel
        if (modalidade.equals("MOTOBOY") && pesoTotal.compareTo(LIMITE_MOTOBOY) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Calcular frete (antes de validar cupom, pois FRETEGRATIS depende dele)
        BigDecimal frete;
        int prazo;
        switch (modalidade) {
            case "ECONOMICA" -> {
                frete = new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotal));
                prazo = 7;
            }
            case "EXPRESSA" -> {
                frete = new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotal));
                prazo = 2;
            }
            case "RETIRADA_LOJA" -> {
                frete = BigDecimal.ZERO;
                prazo = 1;
            }
            case "MOTOBOY" -> {
                frete = new BigDecimal("18.00");
                prazo = 0;
            }
            default -> throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        if (nivel.equals("OURO")) {
            frete = BigDecimal.ZERO;
        }
        frete = round(frete);

        // 6-7. Cupom
        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (req.cupom != null && !req.cupom.isEmpty()) {
            if (!CUPONS.contains(req.cupom)) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            switch (req.cupom) {
                case "BEMVINDO10" -> descontoCupom = subtotal.multiply(new BigDecimal("0.10"));
                case "MENOS50" -> {
                    if (subtotal.compareTo(MIN_MENOS50) < 0) {
                        throw new CheckoutException("CUPOM_NAO_APLICAVEL");
                    }
                    descontoCupom = new BigDecimal("50.00");
                }
                case "FRETEGRATIS" -> descontoCupom = frete;
                case "LEVE3PAGUE2" -> {
                    BigDecimal total = BigDecimal.ZERO;
                    for (CheckoutRequest.Item item : req.itens) {
                        int gratis = item.quantidade / 3;
                        if (gratis > 0) {
                            total = total.add(item.precoUnitario.multiply(BigDecimal.valueOf(gratis)));
                        }
                    }
                    descontoCupom = total;
                }
            }
            descontoCupom = round(descontoCupom);
        }

        // Seguro
        BigDecimal pct = SEGURO_REGIAO.get(regiao);
        BigDecimal seguro = round(subtotal.multiply(pct));

        // Total do pedido
        BigDecimal totalPedido = round(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 8. Forma de pagamento
        if (req.formaPagamento == null || !FORMAS.contains(req.formaPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        String forma = req.formaPagamento;
        int parcelas = req.parcelas == null ? 1 : req.parcelas;

        // 9. Parcelamento
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

        // 10. Forma disponivel
        if (forma.equals("BOLETO") && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Ajuste pagamento
        BigDecimal ajuste;
        BigDecimal valorFinal;
        BigDecimal valorParcela;
        switch (forma) {
            case "PIX" -> {
                BigDecimal desc = round(totalPedido.multiply(DESCONTO_PIX));
                ajuste = desc.negate();
                valorFinal = round(totalPedido.subtract(desc));
                valorParcela = valorFinal;
            }
            case "BOLETO" -> {
                ajuste = TARIFA_BOLETO;
                valorFinal = round(totalPedido.add(TARIFA_BOLETO));
                valorParcela = valorFinal;
            }
            case "CARTAO" -> {
                if (parcelas <= 3) {
                    ajuste = BigDecimal.ZERO.setScale(2);
                    valorFinal = totalPedido;
                    valorParcela = round(totalPedido.divide(BigDecimal.valueOf(parcelas),
                            10, RoundingMode.HALF_EVEN));
                } else {
                    BigDecimal um = BigDecimal.ONE;
                    BigDecimal umMaisT = um.add(TAXA_CARTAO);
                    // (1+t)^-n
                    BigDecimal pot = umMaisT.pow(parcelas, MathContext.DECIMAL64);
                    BigDecimal fator = um.subtract(um.divide(pot, MathContext.DECIMAL64));
                    BigDecimal parc = totalPedido.multiply(TAXA_CARTAO)
                            .divide(fator, MathContext.DECIMAL64);
                    valorParcela = round(parc);
                    valorFinal = round(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
                    ajuste = round(valorFinal.subtract(totalPedido));
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        // Credito proxima compra
        BigDecimal credito = round(subtotal.multiply(CREDITO_NIVEL.get(nivel)));

        // Brinde
        boolean brinde = nivel.equals("OURO") && subtotal.compareTo(LIMITE_BRINDE) > 0;

        CheckoutResponse resp = new CheckoutResponse();
        resp.subtotalProdutos = subtotal;
        resp.descontoCupom = round(descontoCupom);
        resp.frete = frete;
        resp.prazoEntregaDias = prazo;
        resp.seguro = seguro;
        resp.ajustePagamento = round(ajuste);
        resp.totalFinal = valorFinal;
        resp.parcelas = parcelas;
        resp.valorParcela = valorParcela;
        resp.creditoProximaCompra = credito;
        resp.brinde = brinde;
        return resp;
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
