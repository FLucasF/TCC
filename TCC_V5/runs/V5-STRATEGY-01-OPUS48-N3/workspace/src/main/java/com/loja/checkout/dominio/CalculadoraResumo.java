package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o resumo da compra. Aqui mora o que é igual em todo pedido: a ordem das
 * verificações e a sequência do cálculo descritas no enunciado. O comportamento
 * que muda de caso para caso fica nas dimensões (entrega, cupom, clube, pagamento).
 */
@Service
public class CalculadoraResumo {

    private final RegistroPorCodigo<ModalidadeEntrega> entregas;
    private final RegistroPorCodigo<Cupom> cupons;
    private final RegistroPorCodigo<NivelClube> niveis;
    private final RegistroPorCodigo<FormaPagamento> pagamentos;

    public CalculadoraResumo(RegistroPorCodigo<ModalidadeEntrega> entregas,
                             RegistroPorCodigo<Cupom> cupons,
                             RegistroPorCodigo<NivelClube> niveis,
                             RegistroPorCodigo<FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        // 1. Itens válidos -> subtotal e peso.
        List<ItemPedido> itens = validarItens(pedido.itens());
        BigDecimal subtotal = Dinheiro.centavos(somaProdutos(itens));
        BigDecimal pesoTotal = somaPeso(itens);

        // 2-5. Clube, região e entrega.
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porNome(pedido.regiao())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = entregas.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pesoTotal)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // Frete efetivo: o do clube ouro é zerado.
        BigDecimal frete = nivel.isentaFrete()
                ? Dinheiro.zero()
                : Dinheiro.centavos(modalidade.frete(pesoTotal));

        // 6-7. Cupom (opcional).
        BigDecimal desconto = calcularDesconto(pedido.cupom(), new ContextoCupom(subtotal, frete, itens));

        // Seguro: só a porcentagem muda; a conta é a mesma.
        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.percentualSeguro()));

        // Total do pedido = produtos - desconto + frete + seguro.
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));

        // 8-10. Pagamento.
        FormaPagamento pagamento = pagamentos.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        BigDecimal credito = Dinheiro.centavos(nivel.credito(subtotal));
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private BigDecimal calcularDesconto(String codigoCupom, ContextoCupom ctx) {
        if (codigoCupom == null) {
            return Dinheiro.zero();
        }
        Cupom cupom = cupons.buscar(codigoCupom)
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(ctx)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(ctx));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validos = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (naoPositivo(item.precoUnitario()) || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
            validos.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal somaProdutos(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.totalItem());
        }
        return total;
    }

    private BigDecimal somaPeso(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.pesoItem());
        }
        return total;
    }
}
