package com.amazoninsight.llmreview.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String amazonReviewID;

    @Column
    private String reviewTitle;

    @Column(columnDefinition = "TEXT")
    private String reviewText;

    @Column
    private Double rating;

    @Column
    private Integer helpfulCount;

    @Column
    private LocalDate reviewDate;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private double[] embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private AmazonItem product;

//    public Review(String amazonReviewID, String reviewTitle, String reviewText, Double rating, Integer helpfulCount, LocalDate reviewDate) {
//        this.amazonReviewID = amazonReviewID;
//        this.reviewTitle = reviewTitle;
//        this.reviewText = reviewText;
//        this.rating = rating;
//        this.helpfulCount = helpfulCount;
//        this.reviewDate = reviewDate;
//    }

}
