package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;

    private int count;
    private List<DetailedSearchResult> data;

    public SearchResponse() {}

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
    public SearchResponse(boolean result, int count) {
        this.result = result;
        this.count = count;
    }
}
