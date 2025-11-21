import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Assume this code is within a service class that has OpenSearchClient autowired
// private final OpenSearchClient openSearchClient; 

public SearchResponse<Void> getPortfolioViewMetrics(OpenSearchClient openSearchClient) throws IOException {

    // --- 1. Define the Scripted Metric Aggregations (Re-usable for both unique counts) ---

    // Define the unique count scripts for Seal ID
    Script sealIdInitScript = new Script.Builder().inline("state.sealIds = new HashSet();").build();
    Script sealIdMapScript = new Script.Builder().inline("if (doc['sealId'].size() > 0) { state.sealIds.add(doc['sealId'].value); }").build();
    Script sealIdCombineScript = new Script.Builder().inline("return state.sealIds;").build();
    Script sealIdReduceScript = new Script.Builder().inline("Set sealIds = new HashSet(); for (s in states) { sealIds.addAll(s); } return sealIds.size();").build();

    // Define the unique count scripts for Dataset ID
    Script datasetIdInitScript = new Script.Builder().inline("state.datasetIds = new HashSet();").build();
    Script datasetIdMapScript = new Script.Builder().inline("if (doc['datasetId'].size() > 0) { state.datasetIds.add(doc['datasetId'].value); }").build();
    Script datasetIdCombineScript = new Script.Builder().inline("return state.datasetIds;").build();
    Script datasetIdReduceScript = new Script.Builder().inline("Set datasetIds = new HashSet(); for (s in states) { datasetIds.addAll(s); } return datasetIds.size();").build();

    // --- 2. Build the Sub-Aggregations (Inner Aggs) ---

    // Aggregation 1: Owner Info (top_hits)
    Aggregation ownerInfoAgg = new Aggregation.Builder()
        .topHits(h -> h
            .size(1)
            .source(s -> s
                .filter(f -> f.includes(Arrays.asList("portfolioOwnerName", "portfolioOwnerSid")))
            )
        )
        .build();

    // Aggregation 2: Unique Seal Count (scripted_metric)
    Aggregation uniqueSealCountAgg = new Aggregation.Builder()
        .scriptedMetric(sm -> sm
            .initScript(sealIdInitScript)
            .mapScript(sealIdMapScript)
            .combineScript(sealIdCombineScript)
            .reduceScript(sealIdReduceScript)
        )
        .build();

    // Aggregation 3: Unique Dataset Count (scripted_metric)
    Aggregation uniqueDatasetCountAgg = new Aggregation.Builder()
        .scriptedMetric(sm -> sm
            .initScript(datasetIdInitScript)
            .mapScript(datasetIdMapScript)
            .combineScript(datasetIdCombineScript)
            .reduceScript(datasetIdReduceScript)
        )
        .build();
    
    // --- 3. Build the Main Aggregation (by_portfolio) ---

    Aggregation byPortfolioAgg = new Aggregation.Builder()
        .terms(t -> t
            .field("portfolioName")
            .size(1000)
        )
        .aggregations(Collections.singletonMap("owner_info", ownerInfoAgg))
        .aggregations(Collections.singletonMap("unique_sealId_count", uniqueSealCountAgg))
        .aggregations(Collections.singletonMap("unique_datasetId_count", uniqueDatasetCountAgg))
        .build();

    // --- 4. Build the Search Request ---

    SearchRequest searchRequest = SearchRequest.of(r -> r
        .index("portfolio_view_index") // Target index
        .size(0) // Don't return any documents
        .aggregations("by_portfolio", byPortfolioAgg) // Add the main aggregation
    );

    // --- 5. Execute the Request and Return Response ---

    // Note: The response is typed as <Void> because we set size(0) and are only
    // interested in the aggregations, which are accessed via response.aggregations()
    return openSearchClient.search(searchRequest, Void.class);
}