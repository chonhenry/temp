import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hits;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TopHitsAggregation;
import org.opensearch.client.opensearch.core.search.TopHitsAggregation.Builder;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.SourceFilter;
import org.opensearch.client.opensearch.indices.IndexSettings.Builder;
import org.opensearch.client.json.JsonData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Assuming OpenSearchClient is injected via constructor
private final OpenSearchClient openSearchClient;

// Constructor...

public Map<String, PortfolioSummary> getPortfolioSummary() throws IOException {
    // 1. Build the Search Request
    SearchRequest searchRequest = SearchRequest.of(r -> r
        .index("portfolio_view_index")
        .size(0) // Equivalent to "size": 0
        .aggregations("by_portfolio", a -> a // Name the top aggregation
            .terms(t -> t
                .field("portfolioName")
                .size(1000) // Equivalent to "size": 1000
            )
            .aggregations("owner_info", oa -> oa // Nested agg 1: Top Hits
                .topHits(th -> th
                    .size(1) // Equivalent to "size": 1
                    // Equivalent to "_source": ["portfolioOwnerName", "portfolioOwnerSid"]
                    .source(sc -> sc 
                        .filter(sf -> sf 
                            .includes(List.of("portfolioOwnerName", "portfolioOwnerSid"))
                        )
                    )
                )
            )
            .aggregations("sealId_count", ca -> ca // Nested agg 2: Cardinality
                .cardinality(c -> c
                    .field("sealId")
                )
            )
            .aggregations("datasetId_count", ca -> ca // Nested agg 3: Cardinality
                .cardinality(c -> c
                    .field("datasetId")
                )
            )
        )
    );

    // 2. Execute the Request
    SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

    // 3. Process the Response
    return processResponse(response);
}