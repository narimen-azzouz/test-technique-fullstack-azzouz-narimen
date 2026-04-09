package com.tendanz.pricing.service;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.PricingRule;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.enums.AgeCategory;
import com.tendanz.pricing.repository.PricingRuleRepository;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for handling pricing and quote calculations.
 * Manages the business logic for pricing rules and quote generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final ProductRepository productRepository;
    private final ZoneRepository zoneRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final QuoteRepository quoteRepository;
    private final ObjectMapper objectMapper;

    /**
     * Calculate a quote based on the provided request.
     *
     * @param request the quote request containing productId, zoneCode, clientName, clientAge
     * @return the calculated quote response
     * @throws IllegalArgumentException if product, zone, or pricing rule not found
     */
    @Transactional
    public QuoteResponse calculateQuote(QuoteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + request.getProductId()));

        String normalizedZoneCode = request.getZoneCode() == null ? null : request.getZoneCode().trim().toUpperCase();
        Zone zone = zoneRepository.findByCode(normalizedZoneCode)
            .orElseThrow(() -> new IllegalArgumentException("Zone not found with code: " + normalizedZoneCode));

        PricingRule pricingRule = pricingRuleRepository.findByProduct_Id(product.getId())
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found for product ID: " + product.getId()));

        AgeCategory ageCategory = AgeCategory.fromAge(request.getClientAge());
        BigDecimal ageFactor = getAgeFactor(pricingRule, ageCategory);

        BigDecimal baseRate = pricingRule.getBaseRate();
        BigDecimal zoneRiskCoefficient = zone.getRiskCoefficient();

        BigDecimal finalPrice = baseRate
                .multiply(ageFactor)
                .multiply(zoneRiskCoefficient)
                .setScale(2, RoundingMode.HALF_UP);

        List<String> appliedRules = new ArrayList<>();
        appliedRules.add("Base rate: " + baseRate.setScale(2, RoundingMode.HALF_UP));
        appliedRules.add("Age category: " + ageCategory + " (factor " + ageFactor + ")");
        appliedRules.add("Zone: " + zone.getCode() + " (risk coefficient " + zoneRiskCoefficient + ")");
        appliedRules.add("Final price = baseRate x ageFactor x zoneRiskCoefficient = " + finalPrice);

        Quote quote = Quote.builder()
                .product(product)
                .zone(zone)
                .clientName(request.getClientName() == null ? null : request.getClientName().trim())
                .clientAge(request.getClientAge())
                .basePrice(baseRate.setScale(2, RoundingMode.HALF_UP))
                .finalPrice(finalPrice)
                .appliedRules(convertRulesToJson(appliedRules))
                .build();

        Quote saved = quoteRepository.save(quote);
        log.info("Created quote id={} for productId={} zoneCode={}", saved.getId(), product.getId(), zone.getCode());

        return mapToResponse(saved, appliedRules);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> getQuotes(Long productId, Double minPrice) {
        BigDecimal minFinalPrice = minPrice == null ? null : BigDecimal.valueOf(minPrice);

        List<Quote> quotes;
        if (productId != null && minFinalPrice != null) {
            quotes = quoteRepository.findByProduct_IdAndFinalPriceGreaterThanEqual(productId, minFinalPrice);
        } else if (productId != null) {
            quotes = quoteRepository.findByProduct_Id(productId);
        } else if (minFinalPrice != null) {
            quotes = quoteRepository.findByFinalPriceGreaterThanEqual(minFinalPrice);
        } else {
            quotes = quoteRepository.findAll();
        }

        return quotes.stream()
                .map(q -> mapToResponse(q, deserializeRules(q.getAppliedRules())))
                .collect(Collectors.toList());
    }

    /**
     * Get the age factor for a specific age category from a pricing rule.
     * This helper is provided — use it in your calculateQuote implementation.
     *
     * @param pricingRule the pricing rule containing age factors
     * @param ageCategory the age category (YOUNG, ADULT, SENIOR, ELDERLY)
     * @return the appropriate age factor as BigDecimal
     */
    private BigDecimal getAgeFactor(PricingRule pricingRule, AgeCategory ageCategory) {
        return switch (ageCategory) {
            case YOUNG -> pricingRule.getAgeFactorYoung();
            case ADULT -> pricingRule.getAgeFactorAdult();
            case SENIOR -> pricingRule.getAgeFactorSenior();
            case ELDERLY -> pricingRule.getAgeFactorElderly();
        };
    }

    /**
     * Convert a list of applied rules to a JSON string for storage.
     * This helper is provided — use it in your calculateQuote implementation.
     *
     * @param rules the list of rule descriptions
     * @return the JSON string representation
     */
    private String convertRulesToJson(List<String> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("Error converting rules to JSON", e);
            return "[]";
        }
    }

    /**
     * Convert a Quote entity to a QuoteResponse DTO.
     * This helper is provided — use it in your calculateQuote implementation.
     *
     * @param quote the quote entity
     * @param appliedRules the list of applied rules
     * @return the quote response DTO
     */
    private QuoteResponse mapToResponse(Quote quote, List<String> appliedRules) {
        return QuoteResponse.builder()
                .quoteId(quote.getId())
                .productName(quote.getProduct().getName())
                .zoneName(quote.getZone().getName())
                .clientName(quote.getClientName())
                .clientAge(quote.getClientAge())
                .basePrice(quote.getBasePrice())
                .finalPrice(quote.getFinalPrice())
                .appliedRules(appliedRules)
                .createdAt(quote.getCreatedAt())
                .build();
    }

    /**
     * Get a quote by ID.
     * This method is provided as a reference for how to retrieve and return quotes.
     *
     * @param id the quote ID
     * @return the quote response
     * @throws IllegalArgumentException if quote not found
     */
    public QuoteResponse getQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with ID: " + id));

        List<String> appliedRules = deserializeRules(quote.getAppliedRules());
        return mapToResponse(quote, appliedRules);
    }

    /**
     * Deserialize the rules JSON string back to a list.
     *
     * @param rulesJson the JSON string
     * @return the list of rules
     */
    private List<String> deserializeRules(String rulesJson) {
        try {
            return objectMapper.readValue(rulesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Error deserializing rules from JSON", e);
            return new ArrayList<>();
        }
    }
}
