package com.pousada.service;

import com.pousada.document.DocumentContent;
import com.pousada.document.DocumentRendererFactory;
import com.pousada.domain.Reservation;

public class DocumentService {
    private final DocumentRendererFactory factory;

    public DocumentService(DocumentRendererFactory factory) {
        this.factory = factory;
    }

    public DocumentContent generateDocument(Reservation reservation) {
        var renderer = factory.getRenderer(reservation);
        return renderer.render(reservation);
    }
}
