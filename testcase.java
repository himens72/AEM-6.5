
package com.kpmg.core.models.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UniversalFilteringESApiResponse {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private int totalResults;
    private List<UniversalFilterCard> cards = List.of();
    private List<TagBucket> tagBuckets = List.of();

    // Flatten Elasticsearch hits
    @JsonSetter("hits")
    public void setHits(JsonNode hits)
            throws JsonProcessingException {

        totalResults = hits.path("total")
                           .path("value")
                           .asInt(0);

        List<UniversalFilterCard> results =
                new ArrayList<>();

        JsonNode hitArray = hits.path("hits");

        if (hitArray.isArray()) {
            for (JsonNode hit : hitArray) {
                JsonNode source = hit.path("_source");

                if (source.isObject()) {
                    results.add(MAPPER.treeToValue(
                            source,
                            UniversalFilterCard.class
                    ));
                }
            }
        }

        cards = List.copyOf(results);
    }

    // Flatten Elasticsearch aggregations
    @JsonSetter("aggregations")
    public void setAggregations(JsonNode aggregations)
            throws JsonProcessingException {

        List<TagBucket> buckets = new ArrayList<>();

        JsonNode bucketArray = aggregations
                .path("tags_aggregation")
                .path("buckets");

        if (bucketArray.isArray()) {
            for (JsonNode bucket : bucketArray) {
                buckets.add(MAPPER.treeToValue(
                        bucket,
                        TagBucket.class
                ));
            }
        }

        tagBuckets = List.copyOf(buckets);
    }

    public int getTotalResults() {
        return totalResults;
    }

    public List<UniversalFilterCard> getCards() {
        return cards;
    }

    public List<TagBucket> getTagBuckets() {
        return tagBuckets;
    }

    // Card data from _source
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UniversalFilterCard {

        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("qualified_url")
        private String outsideUrl;

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

        @JsonProperty("original_pub_date")
        private String originalPubDate;

        @JsonProperty("formatted_filter_date")
        private String formattedFilterDate;

        @JsonProperty("formatted_article_date")
        private String formattedArticleDate;

        @JsonProperty("formatted_original_pub_date")
        private String formattedOriginalPubDate;

        @JsonProperty("cqTags")
        private List<String> cqTags;

        @JsonProperty("cqTagsWithTitle")
        private List<String> cqTagsWithTitle;

        public String getDocumentId() {
            return documentId;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getOutsideUrl() {
            return outsideUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getImageAltText() {
            return imageAltText;
        }

        public Integer getFilterYear() {
            return filterYear;
        }

        public String getDocumentType() {
            return documentType;
        }

        public String getArticleType() {
            return articleType;
        }

        public String getOriginalPubDate() {
            return originalPubDate;
        }

        public String getFormattedFilterDate() {
            return formattedFilterDate;
        }

        public String getFormattedArticleDate() {
            return formattedArticleDate;
        }

        public String getFormattedOriginalPubDate() {
            return formattedOriginalPubDate;
        }

        public List<String> getCqTags() {
            return cqTags;
        }

        public List<String> getCqTagsWithTitle() {
            return cqTagsWithTitle;
        }
    }

    // Aggregation filter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagBucket {

        private String key;

        @JsonProperty("doc_count")
        private int docCount;

        public String getKey() {
            return key;
        }

        public int getDocCount() {
            return docCount;
        }
    }
}
