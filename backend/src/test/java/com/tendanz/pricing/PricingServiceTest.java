package com.tendanz.pricing;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.PricingRule;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.repository.PricingRuleRepository;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import com.tendanz.pricing.service.PricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PricingService.
 */
@DataJpaTest
@Import({PricingService.class, ObjectMapper.class})
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PricingServiceTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    private Product product;
    private Zone zone;
    private PricingRule pricingRule;

    @BeforeEach
    void setUp() {
        quoteRepository.deleteAllInBatch();
        pricingRuleRepository.deleteAllInBatch();
        zoneRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        // Test data: Auto Insurance, zone coefficient 1.20, standard age factors
        product = Product.builder()
                .name("Test Auto Insurance")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(product);

        zone = Zone.builder()
            .code("TUN_TEST")
                .name("Grand Tunis")
                .riskCoefficient(BigDecimal.valueOf(1.20))
                .build();
        zoneRepository.save(zone);

        pricingRule = PricingRule.builder()
                .product(product)
                .baseRate(BigDecimal.valueOf(500.00))
                .ageFactorYoung(BigDecimal.valueOf(1.30))
                .ageFactorAdult(BigDecimal.valueOf(1.00))
                .ageFactorSenior(BigDecimal.valueOf(1.20))
                .ageFactorElderly(BigDecimal.valueOf(1.50))
                .createdAt(LocalDateTime.now())
                .build();
        pricingRuleRepository.save(pricingRule);
    }

    /**
     * TODO: Test quote calculation for an adult client (age 25-45).
     *
     * Expected: 500.00 × 1.00 (adult) × 1.20 (Tunis) = 600.00 TND
     */
    @Test
    void testCalculateQuoteForAdult() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode(zone.getCode())
                .clientName("Alice")
                .clientAge(30)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertNotNull(response.getQuoteId());
        assertEquals("Test Auto Insurance", response.getProductName());
        assertEquals("Grand Tunis", response.getZoneName());
        assertEquals("Alice", response.getClientName());
        assertEquals(30, response.getClientAge());
        assertEquals(0, response.getBasePrice().compareTo(new BigDecimal("500.00")));
        assertEquals(0, response.getFinalPrice().compareTo(new BigDecimal("600.00")));
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getAppliedRules());
        assertFalse(response.getAppliedRules().isEmpty());
    }

    /**
     * TODO: Test quote calculation for a young client (age 18-24).
     *
     * Expected: 500.00 × 1.30 (young) × 1.20 (Tunis) = 780.00 TND
     */
    @Test
    void testCalculateQuoteForYoungClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode(zone.getCode())
                .clientName("Bob")
                .clientAge(20)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);
        assertNotNull(response);
        assertEquals(0, response.getFinalPrice().compareTo(new BigDecimal("780.00")));
    }

    /**
     * TODO: Test quote calculation for a senior client (age 46-65).
     *
     * Expected: 500.00 × 1.20 (senior) × 1.20 (Tunis) = 720.00 TND
     */
    @Test
    void testCalculateQuoteForSeniorClient() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode(zone.getCode())
                .clientName("Charlie")
                .clientAge(50)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);
        assertNotNull(response);
        assertEquals(0, response.getFinalPrice().compareTo(new BigDecimal("720.00")));
    }

    /**
     * TODO: Test that requesting a quote with an invalid product ID
     * throws IllegalArgumentException.
     */
    @Test
    void testCalculateQuoteWithInvalidProductId() {
        QuoteRequest request = QuoteRequest.builder()
            .productId(999999L)
            .zoneCode(zone.getCode())
                        .clientName("Alice")
            .clientAge(30)
            .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> pricingService.calculateQuote(request));
        assertTrue(ex.getMessage().toLowerCase().contains("product"));
    }

    /**
     * TODO: Test that requesting a quote with an invalid zone code
     * throws IllegalArgumentException.
     */
    @Test
    void testCalculateQuoteWithInvalidZoneCode() {
        QuoteRequest request = QuoteRequest.builder()
            .productId(product.getId())
            .zoneCode("XXX")
                        .clientName("Alice")
            .clientAge(30)
            .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> pricingService.calculateQuote(request));
        assertTrue(ex.getMessage().toLowerCase().contains("zone"));
    }

    @Test
    void testGetQuoteById() {
        QuoteRequest request = QuoteRequest.builder()
            .productId(product.getId())
            .zoneCode(zone.getCode())
            .clientName("Frank")
            .clientAge(25)
            .build();

        QuoteResponse created = pricingService.calculateQuote(request);
        QuoteResponse fetched = pricingService.getQuote(created.getQuoteId());

        assertNotNull(fetched);
        assertEquals(created.getQuoteId(), fetched.getQuoteId());
        assertEquals(created.getProductName(), fetched.getProductName());
        assertEquals(created.getZoneName(), fetched.getZoneName());
        assertEquals(created.getClientName(), fetched.getClientName());
        assertEquals(created.getClientAge(), fetched.getClientAge());
        assertEquals(0, created.getFinalPrice().compareTo(fetched.getFinalPrice()));
        assertNotNull(fetched.getAppliedRules());
        assertFalse(fetched.getAppliedRules().isEmpty());
    }
}
