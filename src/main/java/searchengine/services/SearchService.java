package searchengine.services;

import searchengine.dto.search.SearchQuery;
import searchengine.dto.search.SearchResponse;


public interface SearchService {
    SearchResponse search(SearchQuery searchQuery);
}
