package com.tendanz.pricing.repository;

import com.tendanz.pricing.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for Quote entity.
 * Provides database operations for quotes.
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByClientName(String clientName);

    List<Quote> findByProduct_Id(Long productId);

    List<Quote> findByProduct_IdAndFinalPriceGreaterThanEqual(Long productId, BigDecimal minFinalPrice);

    @Query("select q from Quote q where q.finalPrice >= :minPrice")
    List<Quote> findWithFinalPriceAbove(@Param("minPrice") BigDecimal minPrice);

}
