
package com.kpmg.core.models.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchHits {

    private List<Hit> hits = new ArrayList<>();

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

        private List<Long> sort;
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

        private List<String> cqTags = new ArrayList<>();
        private List<String> cqTagsWithTitle = new ArrayList<>();
    }
}
