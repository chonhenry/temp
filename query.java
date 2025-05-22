import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
 import org.opensearch.client.opensearch._types.query_dsl.Query;
 import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
 import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
 import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;
 import org.opensearch.client.opensearch._types.FieldValue;
 import org.opensearch.client.opensearch._types.FieldValue.Builder;

 import java.util.Arrays;
 import java.util.List;

 public class OpenSearchQueryExample {

  public static Query buildProductQuery() {

  // Condition 1: Origin country is either America or Mexico
  List<FieldValue> countries = Arrays.asList(FieldValue.of("America"), FieldValue.of("Mexico"));
  Query originCountryQuery = TermsQuery.of(t -> t
  .field("origin_country.keyword")
  .terms(v -> v.value(countries))
  )._toQuery();

  // Sub-condition A: Review is "good"
  Query goodReviewQuery = TermQuery.of(t -> t
  .field("review.keyword")
  .value("good")
  )._toQuery();

  // Sub-condition B: The "review" field does not exist (i.e., no review)
  Query noReviewQuery = BoolQuery.of(b -> b
  .mustNot(m -> m.exists(
  ExistsQuery.of(e -> e.field("review"))))
  )._toQuery();

  // Condition 2: Review is either "good" OR it has no review
  Query reviewQuery = BoolQuery.of(b -> b
  .should(goodReviewQuery)
  .should(noReviewQuery)
  )._toQuery();

  // Combine both conditions using a bool query with "must"
  Query finalQuery = BoolQuery.of(b -> b
  .must(originCountryQuery)
  .must(reviewQuery)
  )._toQuery();

  return finalQuery;
  }

  public static void main(String[] args) {
  Query query = buildProductQuery();
  // At this point, you would use your OpenSearch client to execute this query
  // against your "product_index". This part depends on how you've set up
  // your client.
  System.out.println(query.jsonValue().toString()); // For demonstration, prints the JSON query
  }
 }
