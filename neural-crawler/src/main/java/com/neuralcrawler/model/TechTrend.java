package com.neuralcrawler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tech_trends")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TechTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String canonicalName;

    private String rawName;

    @Enumerated(EnumType.STRING)
    private TechCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrendSource source;

    private String sourceUrl;

    private Long starCount;
    private Integer mentionCount;
    private Long downloadCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tech_trend_tags", joinColumns = @JoinColumn(name = "trend_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String snapshotId;

    @Column(nullable = false)
    private LocalDateTime snapshotAt;

    /**
     * Factory method used by parsers to create a new unsaved TechTrend record.
     * canonicalName is left as rawName initially — NormalizationService sets it properly.
     */
    public static TechTrend of(String rawName, TrendSource source, String snapshotId) {
        return TechTrend.builder()
                .rawName(rawName)
                .canonicalName(rawName)
                .source(source)
                .snapshotId(snapshotId)
                .snapshotAt(LocalDateTime.now())
                .tags(new ArrayList<>())
                .build();
    }
}
