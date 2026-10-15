package com.loja.checkout.domain;

import com.loja.checkout.domain.clube.ClubeResolver;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomResolver;
import com.loja.checkout.domain.cupom.DadosCupom;
import com.loja.checkout.domain.entrega.EntregaResolver;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.PagamentoResolver;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.seguro.Regiao;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra. A ordem de calculo e a de validacao sao as do
 * enunciado; cada regra de negocio vive na sua propria peca (entrega, cupom,
 * clube, pagamento, regiao), e aqui elas apenas sao postas na ordem certa.
 */
@Service
public class CheckoutService {

    private final ClubeResolver clubeResolver;
    private final EntregaResolver entregaResolver;
    private final CupomResolver cupomResolver;
    private final PagamentoResolver pagamentoResolver;

    public CheckoutService(ClubeResolver clubeResolver,
                           EntregaResolver entregaResolver,
                           CupomResolver cupomResolver,
                           PagamentoResolver pagamentoResolver) {
        this.clubeResolver = clubeResolver;
        this.entregaResolver = entregaResolver;
        this.cupomResolver = cupomResolver;
        this.pagamentoResolver = pagamentoResolver;
    }

    public ResumoResponse resumir(ResumoRequest req) {
        // 1. Itens validos -> subtotal e peso do pedido.
        List<ItemPedido> itens = validarItens(req.itens());
        BigDecimal subtotal = Dinheiro.centavos(somaProdutos(itens));
        BigDecimal peso = somaPeso(itens);

        // 2-3. Quem e o cliente.
        NivelClube nivel = clubeResolver.resolver(req.nivelClube());
        Regiao regiao = Regiao.resolver(req.regiao());

        // 4-5. Como recebe.
        ModalidadeEntrega entrega = entregaResolver.resolver(req.modalidadeEntrega());
        entrega.validarDisponibilidade(peso);

        // 6-7. Cupom.
        BigDecimal frete = nivel.aplicarFrete(entrega.frete(peso));
        Cupom cupom = cupomResolver.resolver(req.cupom());
        DadosCupom dadosCupom = new DadosCupom(subtotal, itens, frete);
        cupom.validarAplicabilidade(dadosCupom);
        BigDecimal desconto = cupom.desconto(dadosCupom);

        // Total do pedido: produtos - desconto + frete + seguro.
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(desconto).add(frete).add(seguro));

        // 8-10. Pagamento.
        FormaPagamento pagamento = pagamentoResolver.resolver(req.formaPagamento());
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        pagamento.validarParcelas(parcelas);
        pagamento.validarDisponibilidade(totalPedido);
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validos = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
            validos.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somaProdutos(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalLinha());
        }
        return soma;
    }

    private BigDecimal somaPeso(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoLinha());
        }
        return soma;
    }
}
