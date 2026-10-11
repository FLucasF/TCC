package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.PorCodigo;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.web.ResumoCompraRequest;
import com.loja.checkout.web.ResumoCompraResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra. Aqui mora o que e igual em todo pedido: a ordem de
 * conferencia dos erros e a ordem do calculo. O que muda de caso para caso
 * (entrega, cupom, pagamento, clube, regiao) e decidido por cada familia.
 */
@Service
public class ResumoCompraService {

    public ResumoCompraResponse calcular(ResumoCompraRequest req) {
        List<ItemPedido> itens = validarItens(req.itens());
        BigDecimal subtotal = Dinheiro.centavos(somarValores(itens));
        BigDecimal peso = somarPesos(itens);

        NivelClube nivel = resolver(NivelClube.class, req.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = resolver(Regiao.class, req.regiao(), CodigoErro.REGIAO_INVALIDA);

        ModalidadeEntrega modalidade =
                resolver(ModalidadeEntrega.class, req.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        if (!modalidade.atende(peso)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Cupom cupom = resolverCupom(req.cupom());
        if (cupom != null && !cupom.aplicavel(subtotal)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        FormaPagamento forma =
                resolver(FormaPagamento.class, req.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal frete = calcularFrete(modalidade, nivel, peso);
        BigDecimal desconto = calcularDesconto(cupom, subtotal, frete, itens);
        BigDecimal seguro = Dinheiro.centavos(regiao.seguro(subtotal));
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!forma.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = forma.liquidar(totalPedido, parcelas);
        BigDecimal ajuste = pagamento.totalFinal().subtract(totalPedido);
        BigDecimal credito = Dinheiro.centavos(nivel.credito(subtotal));

        return new ResumoCompraResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                credito,
                nivel.temBrinde(subtotal));
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, NivelClube nivel, BigDecimal peso) {
        if (nivel.isentaFrete()) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
        return Dinheiro.centavos(modalidade.custo(peso));
    }

    private BigDecimal calcularDesconto(Cupom cupom, BigDecimal subtotal, BigDecimal frete, List<ItemPedido> itens) {
        if (cupom == null) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
        return Dinheiro.centavos(cupom.desconto(new Cupom.Contexto(subtotal, frete, itens)));
    }

    private List<ItemPedido> validarItens(List<ResumoCompraRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validos = new ArrayList<>();
        for (ResumoCompraRequest.Item item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
            validos.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal somarValores(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPesos(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <E extends Enum<E>> E resolver(Class<E> tipo, String codigo, CodigoErro erro) {
        return PorCodigo.resolver(tipo, codigo)
                .orElseThrow(() -> new PedidoRecusadoException(erro));
    }

    private Cupom resolverCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return PorCodigo.resolver(Cupom.class, codigo)
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.CUPOM_INVALIDO));
    }
}
