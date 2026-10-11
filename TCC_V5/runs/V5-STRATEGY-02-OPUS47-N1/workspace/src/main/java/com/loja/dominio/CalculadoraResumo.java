package com.loja.dominio;

import com.loja.dominio.cupom.Cupom;
import com.loja.dominio.cupom.Cupons;
import com.loja.dominio.modalidade.ModalidadeEntrega;
import com.loja.dominio.modalidade.Modalidades;
import com.loja.dominio.nivel.NivelClube;
import com.loja.dominio.nivel.Niveis;
import com.loja.dominio.pagamento.FormaPagamento;
import com.loja.dominio.pagamento.Pagamentos;
import com.loja.dominio.pagamento.ResultadoPagamento;
import com.loja.web.ItemDTO;
import com.loja.web.PedidoDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CalculadoraResumo {

    public Resumo calcular(PedidoDTO pedido) {
        List<ItemCarrinho> itens = validarItens(pedido.itens());
        NivelClube nivel = Niveis.parse(pedido.nivelClube());
        Regiao regiao = Regiao.parse(pedido.regiao());
        ModalidadeEntrega modalidade = Modalidades.parse(pedido.modalidadeEntrega());

        BigDecimal peso = itens.stream()
            .map(ItemCarrinho::pesoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!modalidade.atende(peso)) throw new ErroCheckout("MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = Dinheiro.centavos(
            itens.stream().map(ItemCarrinho::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        BigDecimal frete = nivel.freteGratis()
            ? Dinheiro.ZERO
            : Dinheiro.centavos(modalidade.calcularFrete(peso));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (pedido.cupom() != null) {
            Cupom cupom = Cupons.parse(pedido.cupom());
            if (!cupom.aplicavel(itens, subtotal)) throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            descontoCupom = Dinheiro.centavos(cupom.desconto(itens, subtotal, frete));
        }

        FormaPagamento forma = Pagamentos.parse(pedido.formaPagamento());
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.parcelasValidas(parcelas)) throw new ErroCheckout("PARCELAMENTO_INVALIDO");

        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.percentual()));
        BigDecimal totalPedido = Dinheiro.centavos(
            subtotal.subtract(descontoCupom).add(frete).add(seguro)
        );

        if (!forma.atende(totalPedido)) throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento rp = forma.calcular(totalPedido, parcelas);
        BigDecimal credito = Dinheiro.centavos(subtotal.multiply(nivel.taxaCredito()));
        boolean brinde = nivel.ganhaBrinde(subtotal);

        return new Resumo(
            subtotal,
            descontoCupom,
            frete,
            modalidade.prazoDias(),
            seguro,
            rp.ajuste(),
            rp.totalFinal(),
            parcelas,
            rp.valorParcela(),
            credito,
            brinde
        );
    }

    private List<ItemCarrinho> validarItens(List<ItemDTO> itens) {
        if (itens == null || itens.isEmpty()) throw new ErroCheckout("PEDIDO_INVALIDO");
        List<ItemCarrinho> result = new ArrayList<>(itens.size());
        for (ItemDTO i : itens) {
            if (i == null
                || i.precoUnitario() == null
                || i.quantidade() == null
                || i.pesoKg() == null
                || i.precoUnitario().signum() <= 0
                || i.quantidade() <= 0
                || i.pesoKg().signum() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            result.add(new ItemCarrinho(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()));
        }
        return result;
    }
}
