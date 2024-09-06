package com.amazoninsight.llmreview.repository;

import com.amazoninsight.llmreview.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByIdIn(List<Long> ids);
}