package com.loja.orderservice.listener;

import com.loja.orderservice.service.CatalogService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener {
    private final CatalogService catalogService;

    public ApplicationStartupListener(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStartup() {
        catalogService.initializeCatalog();
    }
}
