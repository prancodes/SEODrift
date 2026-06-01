package com.seo.project.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "keyword_trends",
    uniqueConstraints = @UniqueConstraint(columnNames = {"keyword", "recordedDate"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyword;

    private Integer videoCountLastMonth;

    private Integer videoCountThisMonth;

    private Double growthRate;

    @Column(nullable = false)
    private LocalDate recordedDate;
}
