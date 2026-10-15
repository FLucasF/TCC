package com.loja.checkout.service;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.Money;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o resumo da compra do checkout.
 *
 * As etapas e as regras de negocio estao descritas junto de cada passo. As
 * verificacoes de erro seguem exatamente a ordem definida pelo negocio e, no
 * primeiro problema encontrado, o pedido e recusado com o codigo correspondente.
 */
@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Itens do carrinho.
        List<ItemPedido> itens = validarItens(req.getItens());

        // 2. Nivel do clube.
        NivelClube nivel = NivelClube.fromCodigo(req.getNivelClube());
        if (nivel == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        // 3. Regiao do cliente.
        Regiao regiao = Regiao.fromCodigo(req.getRegiao());
        if (regiao == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }

        // 4. Modalidade de entrega.
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(req.getModalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }

        // 5. A modalidade atende o peso do pedido? (ex.: motoboy ate 5 kg)
        BigDecimal pesoPedido = somarPeso(itens);
        if (!modalidade.atende(pesoPedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // Valores base do pedido.
        BigDecimal subtotalProdutos = somarProdutos(itens);

        // Frete: calculado pela modalidade; o cliente OURO nunca paga frete.
        BigDecimal frete = nivel.isFreteGratis()
                ? Money.zero()
                : modalidade.calcularFrete(pesoPedido);

        // 6 e 7. Cupom (opcional): precisa existir e ser aplicavel ao pedido.
        BigDecimal descontoCupom = Money.zero();
        String codigoCupom = req.getCupom();
        if (codigoCupom != null && !codigoCupom.isBlank()) {
            Cupom cupom = Cupom.fromCodigo(codigoCupom);
            if (cupom == null) {
                throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
            }
            if (!cupom.aplicavel(subtotalProdutos)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, itens, frete);
        }

        // Seguro: porcentagem por regiao sobre o valor dos produtos.
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        // Total do pedido = produtos - desconto + frete + seguro.
        BigDecimal totalPedido = Money.cents(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        // 8. Forma de pagamento.
        FormaPagamento forma = FormaPagamento.fromCodigo(req.getFormaPagamento());
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        // 9. Numero de parcelas (quando nao vem, e 1).
        int parcelas = req.getParcelas() == null ? 1 : req.getParcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        // 10. A forma de pagamento atende o pedido? (ex.: boleto ate R$ 1.000,00)
        if (!forma.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        // Ajuste da forma de pagamento sobre o total do pedido.
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Money.cents(pagamento.totalFinal().subtract(totalPedido));

        // Clube: credito para a proxima compra e eventual brinde.
        BigDecimal credito = nivel.calcularCredito(subtotalProdutos);
        boolean brinde = nivel.ganhaBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                credito,
                brinde);
    }

    /**
     * Valida o carrinho e converte para itens de pedido. Carrinho vazio, ou
     * item com preco, quantidade ou peso ausente/zero/negativo, invalida o pedido.
     */
    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> resultado = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || naoPositivo(item.getPrecoUnitario())
                    || item.getQuantidade() == null || item.getQuantidade() <= 0
                    || naoPositivo(item.getPesoKg())) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            resultado.add(new ItemPedido(
                    item.getNome(),
                    item.getPrecoUnitario(),
                    item.getQuantidade(),
                    item.getPesoKg()));
        }
        return resultado;
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.signum() <= 0;
    }

    private BigDecimal somarProdutos(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalLinha());
        }
        return Money.cents(soma);
    }

    private BigDecimal somarPeso(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoLinha());
        }
        return soma;
    }
}
