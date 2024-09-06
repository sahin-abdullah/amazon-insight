package com.amazoninsight.llmreview.repository;

import com.amazoninsight.llmreview.model.AmazonItem;
import com.amazoninsight.llmreview.service.AmazonItemService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmazonItemRepository extends JpaRepository<AmazonItem, Long> {
    AmazonItem findByAsin(String asin);

    @Query("SELECT i FROM AmazonItem i JOIN FETCH i.reviews WHERE i.asin = :asin")
    AmazonItem findByAsinWithReviews(@Param("asin") String asin);
}