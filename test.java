
package com.myproject.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myproject.core.models.dto.ElasticsearchResponse;
import com.myproject.core.models.dto.SearchHit;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class UniversalFilteringModel {

    private static final Logger LOG =
            LoggerFactory.getLogger(UniversalFilteringModel.class);

    private static final String API_URL =
            "https://kpmg.com/ecsearch/ie_en";

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

    // Dialog fields
    @ValueMapValue
    private String requestId;

    @ValueMapValue
    private String sortOrder;

    @ValueMapValue
    private Integer pageSize;

    @ValueMapValue
    private Integer offset;

    @ValueMapValue
    private String[] filterTags;

    @ValueMapValue
    private String primaryTag;

    @ValueMapValue
    private String[] documentTypes;

    @ValueMapValue
    private Boolean sortByFilterDate;

    // POJO response
    private ElasticsearchResponse elasticsearchResponse;

    // Prevent repeated API calls within this model instance
    private boolean responseLoaded;

    /**
     * Make the Elasticsearch API call on demand.
     */
    private ElasticsearchResponse fetchElasticsearchResponse() {

        try {
            ObjectNode payload =
                    OBJECT_MAPPER.createObjectNode();

            payload.put(
                "id",
                requestId != null && !requestId.isBlank()
                    ? requestId
                    : "universal_filtering_template_onload"
            );

            ObjectNode params = payload.putObject("params");

            params.put(
                "sort",
                "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc"
            );

            params.put(
                "size",
                pageSize != null ? Math.clamp(pageSize, 1, 50) : 6
            );

            params.put(
                "from",
                offset != null ? Math.max(0, offset) : 0
            );

            params.put(
                "primary_tag",
                primaryTag == null ? "" : primaryTag
            );

            params.put(
                "sortByFilterDate",
                sortByFilterDate == null || sortByFilterDate
            );

            ArrayNode tags = params.putArray("filter_tags_1");

            if (filterTags != null) {
                Arrays.stream(filterTags)
                    .filter(tag -> tag != null && !tag.isBlank())
                    .forEach(tags::add);
            }

            ArrayNode types = params.putArray("document_type");

            if (documentTypes != null) {
                Arrays.stream(documentTypes)
                    .filter(type -> type != null && !type.isBlank())
                    .forEach(types::add);
            }

            String requestBody =
                    OBJECT_MAPPER.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                LOG.error(
                    "Elasticsearch API failed: {}",
                    response.statusCode()
                );
                return null;
            }

            // Convert JSON into Java POJO
            return OBJECT_MAPPER.readValue(
                response.body(),
                ElasticsearchResponse.class
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Elasticsearch API request interrupted", e);
        } catch (Exception e) {
            LOG.error("Error fetching Elasticsearch results", e);
        }

        return null;
    }

    /**
     * Load the response only when first requested.
     */
    public synchronized ElasticsearchResponse
            getElasticsearchResponse() {

        if (!responseLoaded) {
            responseLoaded = true;
            elasticsearchResponse = fetchElasticsearchResponse();
        }

        return elasticsearchResponse;
    }

    public List<SearchHit> getSearchResults() {

        ElasticsearchResponse response =
                getElasticsearchResponse();

        return response != null
                ? response.getSearchResults()
                : List.of();
    }

    public int getTotalResults() {

        ElasticsearchResponse response =
                getElasticsearchResponse();

        return response != null
                ? response.getTotalResults()
                : 0;
    }
}



package com.myproject.core.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleDetails {

    @JsonProperty("document_id")
    private String articleId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("qualified_url")
    private String articleUrl;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image_alt_text")
    private String imageAltText;

    @JsonProperty("original_pub_date")
    private String originalPublicationDate;

    @JsonProperty("formatted_original_pub_date")
    private String formattedOriginalPublicationDate;

    @JsonProperty("formatted_filter_date")
    private String formattedFilterDate;

    @JsonProperty("formatted_article_date")
    private String formattedArticleDate;

    @JsonProperty("filter_year")
    private Integer filterYear;

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("article_type")
    private String articleType;

    @JsonProperty("cqTags")
    private List<String> tags;

    @JsonProperty("cqTagsWithTitle")
    private List<String> tagsWithTitle;

    public String getArticleId() { return articleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getArticleUrl() { return articleUrl; }
    public String getImageUrl() { return imageUrl; }
    public String getImageAltText() { return imageAltText; }
    public String getOriginalPublicationDate() { return originalPublicationDate; }
    public String getFormattedOriginalPublicationDate() {
        return formattedOriginalPublicationDate;
    }
    public String getFormattedFilterDate() { return formattedFilterDate; }
    public String getFormattedArticleDate() { return formattedArticleDate; }
    public Integer getFilterYear() { return filterYear; }
    public String getDocumentType() { return documentType; }
    public String getArticleType() { return articleType; }
    public List<String> getTags() {
        return tags != null ? tags : List.of();
    }
    public List<String> getTagsWithTitle() {
        return tagsWithTitle != null ? tagsWithTitle : List.of();
    }
}


package com.myproject.core.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchHit {

    @JsonProperty("_index")
    private String indexName;

    @JsonProperty("_id")
    private String documentId;

    @JsonProperty("_source")
    private ArticleDetails article;

    @JsonProperty("sort")
    private List<Long> sortValues;

    public String getIndexName() {
        return indexName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public ArticleDetails getArticle() {
        return article;
    }

    public List<Long> getSortValues() {
        return sortValues;
    }
}

package com.myproject.core.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchResponse {

    @JsonProperty("hits")
    private SearchHits hits;

    @JsonProperty("aggregations")
    private Aggregations aggregations;

    public SearchHits getHits() {
        return hits;
    }

    public Aggregations getAggregations() {
        return aggregations;
    }

    public int getTotalResults() {
        return hits != null && hits.getTotal() != null
                ? hits.getTotal().getTotalResults() : 0;
    }

    public List<SearchHit> getSearchResults() {
        return hits != null ? hits.getSearchResults() : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchHits {

        @JsonProperty("total")
        private Total total;

        @JsonProperty("hits")
        private List<SearchHit> searchResults;

        public Total getTotal() {
            return total;
        }

        public List<SearchHit> getSearchResults() {
            return searchResults != null ? searchResults : List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Total {

        @JsonProperty("value")
        private int totalResults;

        public int getTotalResults() {
            return totalResults;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aggregations {

        @JsonProperty("tags_aggregation")
        private TagAggregation tagAggregation;

        public TagAggregation getTagAggregation() {
            return tagAggregation;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagAggregation {

        @JsonProperty("buckets")
        private List<TagBucket> tagBuckets;

        public List<TagBucket> getTagBuckets() {
            return tagBuckets != null ? tagBuckets : List.of();
        }
    }
}

ArrayNode tags = params.putArray("filter_tags_1");

if (items != null) {
    items.stream()
        .filter(Objects::nonNull)
        .map(UniversalFilteringItems::getTagHierarchies)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .flatMap(hierarchy -> Stream.concat(
            Stream.ofNullable(hierarchy.getParentTag()),
            hierarchy.getChildTags() == null
                ? Stream.empty()
                : hierarchy.getChildTags().stream()
                    .filter(Objects::nonNull)
                    .map(ChildTagHierarchy::getChildTag)
        ))
        .filter(tag -> !tag.isBlank())
        .distinct()
        .forEach(tags::add);
}
