package com.loja.checkout.service;

import com.loja.checkout.calculator.CupomCalculator;
import com.loja.checkout.calculator.PagamentoCalculator;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    public Object processar(CheckoutRequest request) {
        // Validações em ordem
        String erroValidacao = validar(request);
        if (erroValidacao != null) {
            return new ErrorResponse(erroValidacao);
        }

        try {
            BigDecimal subtotal = calcularSubtotal(request.itens());
            BigDecimal frete = calcularFrete(request);

            CupomCalculator cupomCalc = new CupomCalculator();
            CupomCalculator.Resultado cupomResultado = cupomCalc.calcular(
                request.cupom() != null ? Cupom.valueOf(request.cupom()) : null,
                subtotal,
                request.itens(),
                frete
            );

            BigDecimal desconto = cupomResultado.desconto;
            if (cupomResultado.freteAjustado.signum() >= 0) {
                frete = cupomResultado.freteAjustado;
            }

            // Aplica frete grátis do OURO
            if (request.nivelClube() == NivelClube.OURO) {
                frete = BigDecimal.ZERO;
            }

            int prazo = request.modalidadeEntrega().getPrazo();

            BigDecimal baseSeguro = subtotal;
            BigDecimal seguro = baseSeguro.multiply(request.regiao().getTaxaSeguro());
            seguro = Arredondamento.paraCentavos(seguro);

            BigDecimal totalPedido = subtotal
                .subtract(desconto)
                .add(frete)
                .add(seguro);
            totalPedido = Arredondamento.paraCentavos(totalPedido);

            Integer parcelas = request.parcelas() != null ? request.parcelas() : 1;
            PagamentoCalculator pagamentoCalc = new PagamentoCalculator();
            PagamentoCalculator.Resultado pagamentoResultado = pagamentoCalc.calcular(
                request.formaPagamento(),
                totalPedido,
                parcelas
            );

            BigDecimal totalFinal = pagamentoResultado.totalComAjuste;
            BigDecimal ajustePagamento = pagamentoResultado.ajuste;
            BigDecimal valorParcela = pagamentoResultado.valorParcela;

            // Crédito do clube
            BigDecimal creditoProximaCompra = subtotal.multiply(request.nivelClube().getTaxaCashback());
            creditoProximaCompra = Arredondamento.paraCentavos(creditoProximaCompra);

            // Brinde do OURO
            boolean brinde = request.nivelClube() == NivelClube.OURO && subtotal.compareTo(LIMITE_BRINDE) > 0;

            return new CheckoutResponse(
                Arredondamento.paraCentavos(subtotal),
                Arredondamento.paraCentavos(desconto),
                Arredondamento.paraCentavos(frete),
                prazo,
                seguro,
                ajustePagamento,
                totalFinal,
                pagamentoResultado.parcelas,
                valorParcela,
                creditoProximaCompra,
                brinde
            );
        } catch (Exception e) {
            return new ErrorResponse("ERRO_PROCESSAMENTO");
        }
    }

    private String validar(CheckoutRequest request) {
        // 1. Carrinho válido
        if (request.itens() == null || request.itens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemRequest item : request.itens()) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null ||
                item.precoUnitario().signum() <= 0 ||
                item.quantidade() <= 0 ||
                item.pesoKg().signum() < 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        // 2. Nível do clube
        if (request.nivelClube() == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        // 3. Região
        if (request.regiao() == null) {
            return "REGIAO_INVALIDA";
        }

        // 4. Modalidade de entrega existe
        if (request.modalidadeEntrega() == null) {
            return "MODALIDADE_INVALIDA";
        }

        // 5. Modalidade disponível
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (request.modalidadeEntrega() == ModalidadeEntrega.MOTOBOY && pesoTotal.compareTo(new BigDecimal("5")) > 0) {
            return "MODALIDADE_INDISPONIVEL";
        }

        // 6. Cupom existe
        if (request.cupom() != null) {
            try {
                Cupom.valueOf(request.cupom());
            } catch (IllegalArgumentException e) {
                return "CUPOM_INVALIDO";
            }

            // 7. Cupom aplicável
            if (request.cupom().equals("MENOS50")) {
                BigDecimal subtotal = calcularSubtotal(request.itens());
                if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
                    return "CUPOM_NAO_APLICAVEL";
                }
            }
        }

        // 8. Forma de pagamento existe
        if (request.formaPagamento() == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        // 9. Parcelas válidas
        Integer parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!validarParcelas(request.formaPagamento(), parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        // 10. Forma de pagamento disponível
        if (request.formaPagamento() == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotal(request.itens());
            if (subtotal.compareTo(LIMITE_BOLETO) > 0) {
                return "FORMA_PAGAMENTO_INDISPONIVEL";
            }
        }

        return null;
    }

    private boolean validarParcelas(FormaPagamento forma, Integer parcelas) {
        return switch (forma) {
            case PIX, BOLETO -> parcelas == 1;
            case CARTAO -> parcelas >= 1 && parcelas <= 12;
        };
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal itemTotal = item.precoUnitario().multiply(new BigDecimal(item.quantidade()));
            subtotal = subtotal.add(itemTotal);
        }
        return Arredondamento.paraCentavos(subtotal);
    }

    private BigDecimal calcularFrete(CheckoutRequest request) {
        ModalidadeEntrega modalidade = request.modalidadeEntrega();
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        BigDecimal frete = modalidade.getCustoBases()
            .add(modalidade.getCustoPorKg().multiply(pesoTotal));

        return Arredondamento.paraCentavos(frete);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            BigDecimal pesoItem = item.pesoKg().multiply(new BigDecimal(item.quantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }

    public static class ErrorResponse {
        public String erro;

        public ErrorResponse(String erro) {
            this.erro = erro;
        }

        public String getErro() {
            return erro;
        }
    }
}
