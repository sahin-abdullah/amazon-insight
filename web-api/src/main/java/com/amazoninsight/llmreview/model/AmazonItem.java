package com.amazoninsight.llmreview.model;

import lombok.*;
import jakarta.persistence.*;

import javax.net.ssl.SSLSession;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "amazon_item")
@Getter
@Setter
@AllArgsConstructor
public class AmazonItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String asin;

    @Column
    private String title;

    @Column
    private Double averageRating;

    @Column
    private Integer totalReviewCount;

    @Column
    private Integer totalRatingCount;

    @Column
    private String imageUrl;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String itemFeatures;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Review> reviews;

    public AmazonItem() {
        this.reviews = new ArrayList<>(); // Initialize the list in the constructor
    }

}
