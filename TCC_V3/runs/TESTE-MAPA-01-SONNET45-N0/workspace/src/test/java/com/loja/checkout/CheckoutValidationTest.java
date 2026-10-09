package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutValidationTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void pedidoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void itemComPrecoZero() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Camiseta");
        item.setPrecoUnitario(BigDecimal.ZERO);
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.30"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void motoboySemLimite() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Camiseta");
        item.setPrecoUnitario(new BigDecimal("79.90"));
        item.setQuantidade(20);
        item.setPesoKg(new BigDecimal("0.30"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void cupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Camiseta");
        item.setPrecoUnitario(new BigDecimal("79.90"));
        item.setQuantidade(2);
        item.setPesoKg(new BigDecimal("0.30"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("CUPOMINEXISTENTE");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void cupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Camiseta");
        item.setPrecoUnitario(new BigDecimal("79.90"));
        item.setQuantidade(2);
        item.setPesoKg(new BigDecimal("0.30"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void parcelamentoInvalidoPix() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Camiseta");
        item.setPrecoUnitario(new BigDecimal("79.90"));
        item.setQuantidade(2);
        item.setPesoKg(new BigDecimal("0.30"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void parcelamentoInvalidoCartao() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Camiseta");
        item.setPrecoUnitario(new BigDecimal("79.90"));
        item.setQuantidade(2);
        item.setPesoKg(new BigDecimal("0.30"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(13);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void boletoAcimaDe1000() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Notebook");
        item.setPrecoUnitario(new BigDecimal("3000.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("2.00"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }
}
