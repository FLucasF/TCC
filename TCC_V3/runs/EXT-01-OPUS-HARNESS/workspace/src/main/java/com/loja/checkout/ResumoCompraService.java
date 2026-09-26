package com.loja.checkout;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = pedidoDe(requisicao.itens());
        NivelClube clube = obrigatorio(NivelClube.class, requisicao.nivelClube(), ErroCheckout.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = obrigatorio(Regiao.class, requisicao.regiao(), ErroCheckout.REGIAO_INVALIDA);
        ModalidadeEntrega entrega =
                obrigatorio(ModalidadeEntrega.class, requisicao.modalidadeEntrega(), ErroCheckout.MODALIDADE_INVALIDA);
        exigir(entrega.atende(pedido.pesoKg()), ErroCheckout.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = clube.freteGratis() ? Dinheiro.ZERO : entrega.frete(pedido.pesoKg());

        Optional<Cupom> cupom = opcional(Cupom.class, requisicao.cupom(), ErroCheckout.CUPOM_INVALIDO);
        cupom.ifPresent(c -> exigir(c.aplicavel(pedido, frete), ErroCheckout.CUPOM_NAO_APLICAVEL));
        BigDecimal descontoCupom = cupom.map(c -> c.desconto(pedido, frete)).orElse(Dinheiro.ZERO);

        FormaPagamento pagamento =
                obrigatorio(FormaPagamento.class, requisicao.formaPagamento(), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        exigir(pagamento.parcelamentoPermitido(parcelas), ErroCheckout.PARCELAMENTO_INVALIDO);

        BigDecimal imposto = regiao.imposto(Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom)));
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(imposto));
        exigir(pagamento.atende(totalPedido), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);

        Cobranca cobranca = pagamento.cobranca(totalPedido, parcelas);
        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                imposto,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                clube.creditoProximaCompra(subtotalProdutos),
                clube.brinde(subtotalProdutos));
    }

    private Pedido pedidoDe(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), ErroCheckout.PEDIDO_INVALIDO);
        return new Pedido(itens.stream().map(this::itemDe).toList());
    }

    private ItemPedido itemDe(ItemRequest item) {
        exigir(item != null, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.precoUnitario()), ErroCheckout.PEDIDO_INVALIDO);
        exigir(item.quantidade() != null && item.quantidade() > 0, ErroCheckout.PEDIDO_INVALIDO);
        exigir(positivo(item.pesoKg()), ErroCheckout.PEDIDO_INVALIDO);
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private <E extends Enum<E>> E obrigatorio(Class<E> tipo, String codigo, ErroCheckout erro) {
        exigir(codigo != null, erro);
        return opcional(tipo, codigo, erro).orElseThrow();
    }

    private <E extends Enum<E>> Optional<E> opcional(Class<E> tipo, String codigo, ErroCheckout erro) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(tipo, codigo));
        } catch (IllegalArgumentException naoExiste) {
            throw new CheckoutException(erro);
        }
    }

    private void exigir(boolean condicao, ErroCheckout erro) {
        if (!condicao) {
            throw new CheckoutException(erro);
        }
    }
}
