package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.ContextoCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.ResultadoPagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem combinada com o site. */
@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = montarPedido(request == null ? null : request.itens());

        ModalidadeEntrega entrega = ModalidadeEntrega.resolver(request.modalidadeEntrega());
        BigDecimal pesoKg = pedido.pesoKg();
        if (!entrega.atende(pesoKg)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = Dinheiro.centavos(entrega.frete(pesoKg));

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        Optional<Cupom> cupom = Cupom.resolver(request.cupom());
        ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
        BigDecimal descontoCupom = cupom
                .map(c -> aplicar(c, contexto))
                .orElse(Dinheiro.ZERO);

        BigDecimal totalPedido = Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento pagamento = FormaPagamento.resolver(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!pagamento.parcelamentoPermitido(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(totalPedido)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido)),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela());
    }

    private BigDecimal aplicar(Cupom cupom, ContextoCupom contexto) {
        if (!cupom.aplicavel(contexto)) {
            throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new Pedido(itens.stream().map(this::montarItem).toList());
    }

    private ItemPedido montarItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
