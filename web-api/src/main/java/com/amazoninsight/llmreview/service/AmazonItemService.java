package com.amazoninsight.llmreview.service;

import com.amazoninsight.llmreview.model.AmazonItem;
import com.amazoninsight.llmreview.repository.AmazonItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AmazonItemService {

    private final AmazonItemRepository amazonItemRepository;

    @Autowired
    public AmazonItemService(AmazonItemRepository amazonItemRepository) {
        this.amazonItemRepository = amazonItemRepository;
    }

    public AmazonItem findByAsin(String asin) {
        return amazonItemRepository.findByAsin(asin);
    }

    public AmazonItem findByAsinWithReviews(String asin) {
        return amazonItemRepository.findByAsinWithReviews(asin);
    }
}