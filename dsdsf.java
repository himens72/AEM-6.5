
package com.yourproject.core.models.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UniversalFilteringResponse {

    private HitsData hits = new HitsData();
    private Aggregations aggregations = new Aggregations();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HitsData {

        private Total total = new Total();
        private List<Hit> hits = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Total {

        private long value;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hit {

        @JsonProperty("_index")
        private String index;

        @JsonProperty("_id")
        private String id;

        @JsonProperty("_score")
        private Double score;

        @JsonProperty("_source")
        private Article source;

        private List<Long> sort = new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Article {

        private String title;
        private String description;

        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("original_pub_date")
        private String originalPublishDate;

        @JsonProperty("formatted_filter_date")
        private String formattedFilterDate;

        @JsonProperty("formatted_article_date")
        private String formattedArticleDate;

        @JsonProperty("formatted_original_pub_date")
        private String formattedOriginalPublishDate;

        @JsonProperty("qualified_url")
        private String qualifiedUrl;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("image_alt_text")
        private String imageAltText;

        @JsonProperty("filter_year")
        private Integer filterYear;

        @JsonProperty("document_type")
        private String documentType;

        @JsonProperty("article_type")
        private String articleType;

        @JsonProperty("cqTags")
        private List<String> cqTags = new ArrayList<>();

        @JsonProperty("cqTagsWithTitle")
        private List<String> cqTagsWithTitle =
                new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aggregations {

        @JsonProperty("tags_aggregation")
        private TagsAggregation tagsAggregation =
                new TagsAggregation();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagsAggregation {

        @JsonProperty("doc_count_error_upper_bound")
        private long docCountErrorUpperBound;

        @JsonProperty("sum_other_doc_count")
        private long sumOtherDocCount;

        private List<TagBucket> buckets =
                new ArrayList<>();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagBucket {

        private String key;

        @JsonProperty("doc_count")
        private long docCount;
    }
}
