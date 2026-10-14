
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
public class SearchAggregations {

    @JsonProperty("tags_aggregation")
    private TagsAggregation tagsAggregation;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagsAggregation {

        @JsonProperty("doc_count_error_upper_bound")
        private long docCountErrorUpperBound;

        @JsonProperty("sum_other_doc_count")
        private long sumOtherDocCount;

        private List<TagBucket> buckets = new ArrayList<>();
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
