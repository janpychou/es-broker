package io.sugo.es.broker;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.profile.ProfileResult;
import org.elasticsearch.search.profile.ProfileShardResult;
import org.elasticsearch.search.profile.aggregation.AggregationProfileShardResult;
import org.elasticsearch.search.profile.query.CollectorResult;
import org.elasticsearch.search.profile.query.QueryProfileShardResult;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SearchAPI {
  private RestHighLevelClient client;

  private String indexName;
  private String typeName;
  private String idColumn;
  private String[] searchColumns;
  private String[] includeFields;

  public void search() throws IOException {

    SearchRequest searchRequest = new SearchRequest(indexName);
    searchRequest.types(typeName);
    searchRequest.routing(idColumn);
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.matchAllQuery());

    sourceBuilder.query(QueryBuilders.termQuery("user", "kimchy"));
    sourceBuilder.from(0);
    sourceBuilder.size(5);
    sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

    sourceBuilder.profile(true);

    searchRequest.source(sourceBuilder);

    MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("user", "kimchy");
    matchQueryBuilder.fuzziness(Fuzziness.AUTO);
    matchQueryBuilder.prefixLength(3);
    matchQueryBuilder.maxExpansions(10);

    //    QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("user", "kimchy")
    //        .fuzziness(Fuzziness.AUTO)
    //        .prefixLength(3)
    //        .maxExpansions(10);

    sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
    sourceBuilder.sort(new FieldSortBuilder("_uid").order(SortOrder.ASC));

//    sourceBuilder.fetchSource(false);

    String[] includeFields = new String[]{"title", "user", "innerObject.*"};
    String[] excludeFields = new String[]{"_type"};
    sourceBuilder.fetchSource(includeFields, excludeFields);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    HighlightBuilder highlightBuilder = new HighlightBuilder();
    HighlightBuilder.Field highlightTitle =
        new HighlightBuilder.Field("title");
    highlightTitle.highlighterType("unified");
    highlightBuilder.field(highlightTitle);
    HighlightBuilder.Field highlightUser = new HighlightBuilder.Field("user");
    highlightBuilder.field(highlightUser);
    searchSourceBuilder.highlighter(highlightBuilder);

    SearchResponse searchResponse = client.search(searchRequest);

    RestStatus status = searchResponse.status();
    TimeValue took = searchResponse.getTook();
    Boolean terminatedEarly = searchResponse.isTerminatedEarly();
    boolean timedOut = searchResponse.isTimedOut();

    int totalShards = searchResponse.getTotalShards();
    int successfulShards = searchResponse.getSuccessfulShards();
    int failedShards = searchResponse.getFailedShards();
    for (ShardSearchFailure failure : searchResponse.getShardFailures()) {
      // failures should be handled here
    }

    SearchHits hits = searchResponse.getHits();
    long totalHits = hits.getTotalHits();
    float maxScore = hits.getMaxScore();

    SearchHit[] searchHits = hits.getHits();
    for (SearchHit hit : searchHits) {
      // do something with the SearchHit
      String index = hit.getIndex();
      String type = hit.getType();
      String id = hit.getId();
      float score = hit.getScore();

      String sourceAsString = hit.getSourceAsString();
      Map<String, Object> sourceAsMap = hit.getSourceAsMap();
      String documentTitle = (String) sourceAsMap.get("title");
      List<Object> users = (List<Object>) sourceAsMap.get("user");
      Map<String, Object> innerObject = (Map<String, Object>) sourceAsMap.get("innerObject");

    }

    for (SearchHit hit : hits.getHits()) {
      Map<String, HighlightField> highlightFields = hit.getHighlightFields();
      HighlightField highlight = highlightFields.get("title");
      Text[] fragments = highlight.fragments();
      String fragmentString = fragments[0].string();
    }

    Map<String, ProfileShardResult> profilingResults = searchResponse.getProfileResults();
    for (Map.Entry<String, ProfileShardResult> profilingResult : profilingResults.entrySet()) {
      String key = profilingResult.getKey();
      ProfileShardResult profileShardResult = profilingResult.getValue();

      List<QueryProfileShardResult> queryProfileShardResults = profileShardResult.getQueryProfileResults();
      for (QueryProfileShardResult queryProfileResult : queryProfileShardResults) {
        for (ProfileResult profileResult : queryProfileResult.getQueryResults()) {
          String queryName = profileResult.getQueryName();
          long queryTimeInMillis = profileResult.getTime();
          List<ProfileResult> profiledChildren = profileResult.getProfiledChildren();
        }

        CollectorResult collectorResult = queryProfileResult.getCollectorResult();
        String collectorName = collectorResult.getName();
        Long collectorTimeInMillis = collectorResult.getTime();
        List<CollectorResult> profiledChildren = collectorResult.getProfiledChildren();
      }

      AggregationProfileShardResult aggsProfileResults = profileShardResult.getAggregationProfileResults();
      for (ProfileResult profileResult : aggsProfileResults.getProfileResults()) {
        String aggName = profileResult.getQueryName();
        long aggTimeInMillis = profileResult.getTime();
        List<ProfileResult> profiledChildren = profileResult.getProfiledChildren();
      }
    }
  }
}
