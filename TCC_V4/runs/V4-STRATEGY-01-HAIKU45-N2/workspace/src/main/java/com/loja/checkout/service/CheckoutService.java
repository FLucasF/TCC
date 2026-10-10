package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.strategy.AjustePagamentoCalculador;
import com.loja.checkout.domain.strategy.AjustePagamentoCalculadorFactory;
import com.loja.checkout.domain.strategy.CupomAplicador;
import com.loja.checkout.domain.strategy.CupomAplicadorFactory;
import com.loja.checkout.domain.strategy.FreteCalculador;
import com.loja.checkout.domain.strategy.FreteCalculadorFactory;
import com.loja.checkout.util.Arredondador;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CheckoutService {
    private final ValidadorCheckout validador;

    public CheckoutService() {
        this.validador = new ValidadorCheckout();
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        String erroValidacao = validador.validar(request);
        if (erroValidacao != null) {
            return CheckoutResponse.error(erroValidacao);
        }

        try {
            return executarCalculo(request);
        } catch (Exception e) {
            return CheckoutResponse.error("PEDIDO_INVALIDO");
        }
    }

    private CheckoutResponse executarCalculo(CheckoutRequest request) {
        CheckoutResponse response = new CheckoutResponse();

        List<Item> itens = request.getItens();
        NivelClube nivelClube = NivelClube.parse(request.getNivelClube());
        Regiao regiao = Regiao.parse(request.getRegiao());
        FreteCalculador freteCalc = FreteCalculadorFactory.criar(request.getModalidadeEntrega());
        AjustePagamentoCalculador ajusteCalc = AjustePagamentoCalculadorFactory.criar(request.getFormaPagamento());

        // 1. Subtotal de produtos
        double subtotalProdutos = calcularSubtotal(itens);
        response.setSubtotalProdutos(subtotalProdutos);

        // 2. Desconto do cupom
        double descontoCupom = calcularDescontoCupom(request.getCupom(), itens, subtotalProdutos, 0);
        response.setDescontoCupom(descontoCupom);

        // 3. Frete (com efeito especial para OURO)
        double frete = calcularFrete(nivelClube, freteCalc, itens);
        response.setFrete(frete);
        response.setPrazoEntregaDias(freteCalc.getPrazoEntregaDias());

        // 4. Seguro
        double seguro = calcularSeguro(regiao, subtotalProdutos);
        response.setSeguro(seguro);

        // Validar cupom DEPOIS de calcular frete
        if (request.getCupom() != null) {
            CupomAplicador cupom = CupomAplicadorFactory.criar(request.getCupom());
            if (cupom != null && !cupom.ehAplicavel(itens, subtotalProdutos, frete)) {
                return CheckoutResponse.error("CUPOM_NAO_APLICAVEL");
            }
        }

        // Total temporário para validação de forma de pagamento
        double totalTemp = subtotalProdutos - descontoCupom + frete + seguro;

        // 5. Validar forma de pagamento
        if (!ajusteCalc.ehDisponivel(totalTemp)) {
            return CheckoutResponse.error("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // 6. Ajuste de pagamento
        double ajustePagamento = ajusteCalc.calcularAjuste(totalTemp, request.getParcelas());
        response.setAjustePagamento(ajustePagamento);

        // 7. Total final
        double totalFinal = Arredondador.arredondarParaCentavos(totalTemp + ajustePagamento);
        response.setTotalFinal(totalFinal);

        // 8. Parcelamento
        response.setParcelas(request.getParcelas());
        double valorParcela = calcularValorParcela(totalFinal, request.getFormaPagamento(), request.getParcelas());
        response.setValorParcela(valorParcela);

        // 9. Crédito do próxima compra
        double credito = calcularCredito(nivelClube, subtotalProdutos);
        response.setCreditoProximaCompra(credito);

        // 10. Brinde
        boolean brinde = deveEnviarBrinde(nivelClube, subtotalProdutos);
        response.setBrinde(brinde);

        return response;
    }

    private double calcularSubtotal(List<Item> itens) {
        double subtotal = itens.stream()
                .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
                .sum();
        return Arredondador.arredondarParaCentavos(subtotal);
    }

    private double calcularDescontoCupom(String codigoCupom, List<Item> itens, double subtotal, double frete) {
        if (codigoCupom == null) {
            return 0;
        }

        CupomAplicador cupom = CupomAplicadorFactory.criar(codigoCupom);
        if (cupom == null) {
            return 0;
        }

        return cupom.calcularDesconto(itens, subtotal, frete);
    }

    private double calcularFrete(NivelClube nivelClube, FreteCalculador freteCalc, List<Item> itens) {
        if (nivelClube == NivelClube.OURO) {
            return 0;
        }
        return freteCalc.calcularFrete(itens);
    }

    private double calcularSeguro(Regiao regiao, double subtotal) {
        double seguro = subtotal * regiao.getTaxaSeguro();
        return Arredondador.arredondarParaCentavos(seguro);
    }

    private double calcularValorParcela(double totalFinal, String formaPagamento, int parcelas) {
        if (formaPagamento.equals("CARTAO") && parcelas > 3) {
            // Para cartão com juros, já foi calculado no ajuste
            return Arredondador.arredondarParaCentavos(totalFinal / parcelas);
        }
        return Arredondador.arredondarParaCentavos(totalFinal / parcelas);
    }

    private double calcularCredito(NivelClube nivelClube, double subtotal) {
        return switch (nivelClube) {
            case BRONZE -> 0;
            case PRATA -> {
                double credito = subtotal * 0.02;
                yield Arredondador.arredondarParaCentavos(credito);
            }
            case OURO -> {
                double credito = subtotal * 0.05;
                yield Arredondador.arredondarParaCentavos(credito);
            }
        };
    }

    private boolean deveEnviarBrinde(NivelClube nivelClube, double subtotal) {
        return nivelClube == NivelClube.OURO && subtotal > 500.0;
    }
}
