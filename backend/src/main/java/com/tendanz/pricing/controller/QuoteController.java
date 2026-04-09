package com.tendanz.pricing.controller;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing quotes.
 * Handles all quote-related API endpoints.
 */
@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final PricingService pricingService;

    /** Create a new quote (pricing calculation + persistence). */
    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        log.info("Creating quote for productId={}, zoneCode={}, clientName={}, clientAge={}",
                request.getProductId(), request.getZoneCode(), request.getClientName(), request.getClientAge());
        QuoteResponse response = pricingService.calculateQuote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a quote by ID.
     * This endpoint is provided as a reference implementation.
     *
     * @param id the quote ID
     * @return the quote response
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable Long id) {
        log.info("Fetching quote with ID: {}", id);
        QuoteResponse response = pricingService.getQuote(id);
        return ResponseEntity.ok(response);
    }

    /** List quotes with optional filters (productId, minPrice). */
    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getAllQuotes(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Double minPrice) {
        log.info("Fetching quotes with filters productId={}, minPrice={}", productId, minPrice);
        return ResponseEntity.ok(pricingService.getQuotes(productId, minPrice));
    }
}
