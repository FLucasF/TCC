package com.loja.checkout.web;

import com.loja.checkout.clube.NiveisClube;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadesEntrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Monta o resumo da compra: valida na ordem definida e aplica as etapas do cálculo. */
@Service
public class ResumoService {

    private static final int PARCELAS_PADRAO = 1;

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = pedidoValido(requisicao.itens());
        NivelClube nivel = exigir(NiveisClube.porCodigo(requisicao.nivelClube()), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.porCodigo(requisicao.regiao()), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = exigir(
                ModalidadesEntrega.porCodigo(requisicao.modalidadeEntrega()), CodigoErro.MODALIDADE_INVALIDA);
        exigirQue(modalidade.atende(pedido), CodigoErro.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.frete(pedido);
        BigDecimal desconto = desconto(requisicao.cupom(), pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento forma = exigir(
                FormasPagamento.porCodigo(requisicao.formaPagamento()), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        exigirQue(forma.aceitaParcelas(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);
        exigirQue(forma.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private BigDecimal desconto(String codigoCupom, Pedido pedido, BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(Cupons.porCodigo(codigoCupom), CodigoErro.CUPOM_INVALIDO);
        exigirQue(cupom.aplicavel(pedido), CodigoErro.CUPOM_NAO_APLICAVEL);
        return cupom.desconto(pedido, frete);
    }

    private Pedido pedidoValido(List<ItemRequest> itens) {
        exigirQue(itens != null && !itens.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        return new Pedido(itens.stream().map(this::itemValido).toList());
    }

    private ItemPedido itemValido(ItemRequest item) {
        exigirQue(item != null
                        && positivo(item.precoUnitario())
                        && item.quantidade() != null && item.quantidade() > 0
                        && positivo(item.pesoKg()),
                CodigoErro.PEDIDO_INVALIDO);
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private <T> T exigir(Optional<T> caso, CodigoErro codigo) {
        return caso.orElseThrow(recusa(codigo));
    }

    private void exigirQue(boolean condicao, CodigoErro codigo) {
        if (!condicao) {
            throw recusa(codigo).get();
        }
    }

    private Supplier<PedidoRecusadoException> recusa(CodigoErro codigo) {
        return () -> new PedidoRecusadoException(codigo);
    }
}
