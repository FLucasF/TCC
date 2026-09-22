package com.loja.checkout.servico;

import static com.loja.checkout.dominio.Dinheiro.ZERO;
import static com.loja.checkout.dominio.Dinheiro.arredondar;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroCheckout;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.cupom.CalculoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = pedidoDe(requisicao);
        BigDecimal subtotalProdutos = pedido.subtotalProdutos();

        ModalidadeEntrega entrega = exigir(
                Codigos.procurar(ModalidadeEntrega.class, requisicao.modalidadeEntrega()),
                CodigoErro.MODALIDADE_INVALIDA);
        exigirQue(entrega.atende(pedido.pesoKg()), CodigoErro.MODALIDADE_INDISPONIVEL);
        BigDecimal frete = arredondar(entrega.frete(pedido.pesoKg()));

        BigDecimal descontoCupom = descontoDe(requisicao,
                new CalculoCupom(pedido.itens(), subtotalProdutos, frete));

        FormaPagamento pagamento = exigir(
                Codigos.procurar(FormaPagamento.class, requisicao.formaPagamento()),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Optional.ofNullable(requisicao.parcelas()).orElse(PARCELAS_PADRAO);
        exigirQue(pagamento.permiteParcelas(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);

        BigDecimal totalPedido = arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));
        exigirQue(pagamento.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                arredondar(resultado.totalFinal().subtract(totalPedido)),
                arredondar(resultado.totalFinal()),
                parcelas,
                arredondar(resultado.valorParcela()));
    }

    private Pedido pedidoDe(ResumoRequest requisicao) {
        List<ResumoRequest.ItemRequest> itens = requisicao.itens();
        exigirQue(itens != null && !itens.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        return new Pedido(itens.stream().map(this::itemDe).toList());
    }

    private Item itemDe(ResumoRequest.ItemRequest item) {
        exigirQue(item != null, CodigoErro.PEDIDO_INVALIDO);
        exigirQue(positivo(item.precoUnitario()), CodigoErro.PEDIDO_INVALIDO);
        exigirQue(item.quantidade() != null && item.quantidade() > 0, CodigoErro.PEDIDO_INVALIDO);
        exigirQue(positivo(item.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoDe(ResumoRequest requisicao, CalculoCupom calculo) {
        if (requisicao.cupom() == null) {
            return ZERO;
        }
        Cupom cupom = exigir(
                Codigos.procurar(Cupom.class, requisicao.cupom()), CodigoErro.CUPOM_INVALIDO);
        exigirQue(cupom.aplicavel(calculo), CodigoErro.CUPOM_NAO_APLICAVEL);
        return arredondar(cupom.desconto(calculo));
    }

    private <T> T exigir(Optional<T> encontrado, CodigoErro codigo) {
        return encontrado.orElseThrow(() -> new ErroCheckout(codigo));
    }

    private void exigirQue(boolean condicao, CodigoErro codigo) {
        if (!condicao) {
            throw new ErroCheckout(codigo);
        }
    }
}
