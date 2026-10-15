package com.loja.checkout.api;

import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Opcao;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.Seguro;
import com.loja.checkout.erro.Codigo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Calcula o resumo da compra. Confere os problemas na ordem combinada e segue a
 * ordem do calculo: produtos, desconto do cupom, frete, seguro, total do pedido
 * e ajuste da forma de pagamento.
 */
@Service
public class CalculadoraDeResumo {

    public ResumoResponse calcular(CompraRequest compra) {
        Pedido pedido = Carrinho.montarPedido(compra.itens());
        NivelClube clube = Opcao.exigir(NivelClube.class, compra.nivelClube(), Codigo.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Opcao.exigir(Regiao.class, compra.regiao(), Codigo.REGIAO_INVALIDA);
        ModalidadeEntrega entrega =
                Opcao.exigir(ModalidadeEntrega.class, compra.modalidadeEntrega(), Codigo.MODALIDADE_INVALIDA);
        entrega.exigirQueAtenda(pedido);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = clube.freteCobrado(entrega.frete(pedido));

        Optional<Cupom> cupom = Cupom.informado(compra.cupom());
        cupom.ifPresent(escolhido -> escolhido.exigirQueSeAplique(pedido));
        BigDecimal descontoCupom = cupom
                .map(escolhido -> escolhido.desconto(pedido, frete))
                .orElse(Dinheiro.ZERO);

        BigDecimal seguro = Seguro.doPedido(subtotalProdutos, regiao);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento =
                Opcao.exigir(FormaPagamento.class, compra.formaPagamento(), Codigo.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = compra.parcelasEscolhidas();
        pagamento.exigirParcelas(parcelas);
        pagamento.exigirQueAtenda(totalPedido);
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                Dinheiro.centavos(descontoCupom),
                Dinheiro.centavos(frete),
                entrega.prazoEntregaDias(),
                seguro,
                cobranca.ajuste(totalPedido),
                Dinheiro.centavos(cobranca.totalFinal()),
                parcelas,
                Dinheiro.centavos(cobranca.valorParcela()),
                clube.creditoProximaCompra(subtotalProdutos),
                clube.temBrinde(subtotalProdutos));
    }
}
