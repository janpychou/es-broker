package io.sugo.es.broker;

import com.google.common.base.Preconditions;
import io.sugo.es.broker.dto.IndexResult;
import io.sugo.es.broker.dto.Search;
import io.sugo.es.broker.dto.SearchResult;
import io.sugo.utils.LogUtil;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ElasticSearchManager implements Closeable {

  private static final int BATCH_SIZE = 100;
  private static final ElasticSearchManager manager = new ElasticSearchManager();

  private SqlManager sqlManager;

  private String esIp;
  private int esPort;
  private RestHighLevelClient client;

  private String indexName;
  private String typeName;
  private String idColumn;
  private String[] searchColumns;
  private String[] includeFields;

  private AtomicBoolean closed = new AtomicBoolean(false);

  public static ElasticSearchManager getInstance() {
    return manager;
  }

  public ElasticSearchManager() {
    esIp = SystemConfig.getString(SystemConfig.ES_IP, "localhost");
    esPort = SystemConfig.getInt(SystemConfig.ES_PORT, 9200);
    client = new RestHighLevelClient(RestClient.builder(new HttpHost(esIp, esPort, "http")));
    sqlManager = new SqlManager();
    indexName = SystemConfig.getString(SystemConfig.ES_INDEX_NAME);
    Preconditions.checkNotNull(indexName, SystemConfig.ES_INDEX_NAME);
    typeName = SystemConfig.getString(SystemConfig.ES_TYPE_NAME);
    Preconditions.checkNotNull(typeName, SystemConfig.ES_TYPE_NAME);
    idColumn = SystemConfig.getString(SystemConfig.ES_ID_COLUMN);
    Preconditions.checkNotNull(idColumn, SystemConfig.ES_ID_COLUMN);
    String searchColStr = SystemConfig.getString(SystemConfig.ES_SEARCH_COLUMNS);
    Preconditions.checkNotNull(searchColStr, SystemConfig.ES_SEARCH_COLUMNS);
    searchColumns = searchColStr.split(",");
    String includeFieldStr = SystemConfig.getString(SystemConfig.ES_INCLUDE_FIELDS);
    Preconditions.checkNotNull(includeFieldStr, SystemConfig.ES_INCLUDE_FIELDS);
    includeFields = includeFieldStr.split(",");
    LogUtil.info(String.format("es server:[%s:%d], index:%s, type:%s, id column:%s, search columns:%s",
        esIp, esPort, indexName, typeName, idColumn, searchColStr));
  }

  public IndexResult indexBatch() {
    IndexResult result = new IndexResult(indexName, typeName, idColumn);
    int total = 0;
    int successCount = 0;
    List<Map<String, Object>> jsonMaps = sqlManager.readPaging(total, BATCH_SIZE);
    int jsonCnt = jsonMaps.size();
    total += jsonCnt;
    while (jsonCnt > 0) {
      BulkRequest bulkRequest = new BulkRequest();
      for (int i = 0; i < jsonCnt; i++) {
        Map<String, Object> jsonMap = jsonMaps.get(i);
        IndexRequest indexRequest = new IndexRequest(indexName, typeName, jsonMap.get(idColumn).toString()).source(jsonMap);
        bulkRequest.add(indexRequest);
      }
      int success = sendBulkRequest(bulkRequest);
      result.increaseBach();
      successCount += success;
      if (success != jsonCnt) {
        LogUtil.warn(String.format("index request:[%d], response[%d] successfully", jsonCnt, success));
      }

      jsonMaps = sqlManager.readPaging(total, BATCH_SIZE);
      jsonCnt = jsonMaps.size();
      total += jsonCnt;
    }
    result.finish(total);
    String msg = String.format("index total request:[%d], response[%d] successfully", total, successCount);
    if (successCount != total) {
      LogUtil.warn(msg);
    } else {
      LogUtil.info(msg);
    }
    return result;
  }

  private int sendBulkRequest(BulkRequest bulkRequest) {
    int success = 0;
    bulkRequest.timeout(TimeValue.timeValueMinutes(5));
    bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    try {
      BulkResponse bulkResponse = client.bulk(bulkRequest);
      for (BulkItemResponse item : bulkResponse) {
        if (item.isFailed()) {
          LogUtil.warn(String.format("op:%s for document:[/%s/%s/%s] failed:%s",
              item.getOpType(), item.getIndex(), item.getType(), item.getId(),
              item.getFailureMessage()));
        } else {
          success++;
        }
      }
    } catch (IOException e) {
      LogUtil.error("send bulk request failed", e);
    }
    return success;
  }

  public SearchResult search(final Search search) throws IOException {
    SearchResult searchResult = new SearchResult();

    SearchRequest searchRequest = buildSearchRequest(search);

    SearchResponse searchResponse = client.search(searchRequest);

    return buildSearchResult(searchResponse);
  }

  private SearchRequest buildSearchRequest(Search search) {
    QueryBuilder queryBuilder = buildQuery(search);

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(queryBuilder);
    sourceBuilder.from(search.getOffset());
    sourceBuilder.size(search.getSize());
    sourceBuilder.timeout(new TimeValue(30, TimeUnit.SECONDS));
    sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
    //    sourceBuilder.fetchSource(false);
    //    String[] includeFields = new String[]{"title", "user", "innerObject.*"};
    String[] excludeFields = new String[]{};
    sourceBuilder.fetchSource(includeFields, excludeFields);

    SearchRequest searchRequest = new SearchRequest(indexName);
    searchRequest.types(typeName);
    searchRequest.routing(idColumn);
    searchRequest.source(sourceBuilder);

    HighlightBuilder highlightBuilder = new HighlightBuilder();
    for(String col: searchColumns) {
      HighlightBuilder.Field highlight = new HighlightBuilder.Field(col);
      highlight.highlighterType("unified");
      highlightBuilder.field(highlight);
    }
    sourceBuilder.highlighter(highlightBuilder);

    return searchRequest;
  }

  private QueryBuilder buildQuery(Search search) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    if (search.hasText()) {
      boolQuery.should(QueryBuilders.multiMatchQuery(search.getText(), searchColumns));
    }
    if (search.hasCategory()) {
      boolQuery.should(QueryBuilders.termsQuery(Search.TAG_CATEGORY, search.getCategory()));
    }
    if (search.hasScene()) {
      boolQuery.should(QueryBuilders.termsQuery(Search.TAG_SCENE, search.getScene()));
    }
    if (search.hasStyle()) {
      boolQuery.should(QueryBuilders.termsQuery(Search.TAG_STYLE, search.getStyle()));
    }
    if (search.hasOrigin()) {
      boolQuery.should(QueryBuilders.termsQuery(Search.TAG_ORIGIN, search.getOrigin()));
    }
    return boolQuery;
  }

  private SearchResult buildSearchResult(SearchResponse searchResponse) {
    SearchResult searchResult = new SearchResult();
    RestStatus status = searchResponse.status();
    TimeValue took = searchResponse.getTook();
    boolean terminatedEarly = Boolean.TRUE.equals(searchResponse.isTerminatedEarly());
    boolean timedOut = searchResponse.isTimedOut();
    int failedShards = searchResponse.getFailedShards();

    //    searchResult.setStatus(status.getStatus());
    searchResult.setSpend(took.getMillis());
    //    searchResult.setTerminated(terminatedEarly);
    //    searchResult.setTimeout(timedOut);
    //    searchResult.setFailedShards(failedShards);

    if (failedShards > 0) {
      StringBuilder builder = null;

      for (ShardSearchFailure failure : searchResponse.getShardFailures()) {
        if (builder == null) {
          builder = new StringBuilder("[");
        } else {
          builder.append(",");
        }
        builder.append(String.format("{node id:[%s][%s], index:%s, shard id:%d, failure reason:%s}",
            failure.shard().getNodeId(), failure.shard().getNodeIdText(),
            failure.index(), failure.shardId(), failure.reason()));
      }
      builder.append("]");
      //      searchResult.setFailure(builder.toString());
    }

    if (RestStatus.OK.equals(status)) {
      int totalShards = searchResponse.getTotalShards();
      int successfulShards = searchResponse.getSuccessfulShards();

      SearchHits hits = searchResponse.getHits();
      long totalHits = hits.getTotalHits();
      float maxScore = hits.getMaxScore();

      //      searchResult.setTotalShards(totalShards);
      //      searchResult.setSuccessfulShards(successfulShards);
      searchResult.setTotalHits(totalHits);
      searchResult.setMaxScore(maxScore);

      //      LogUtil.info(String.format("totalShards:[%d], successfulShards:[%d], failedShards:%d, totalHits:%d, maxScore:%f",
      //          totalShards, successfulShards, failedShards, totalHits, maxScore));

      SearchHit[] searchHits = hits.getHits();
      for (SearchHit hit : searchHits) {
        String id = hit.getId();
        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
        sourceAsMap.put(idColumn, id);
        sourceAsMap.put("_score", hit.getScore());
        searchResult.addSource(sourceAsMap);

        Map<String, HighlightField> highlightFields = hit.getHighlightFields();
        for(Map.Entry<String, HighlightField> entry: highlightFields.entrySet()) {
          HighlightField highlight = entry.getValue();
          Text[] fragments = highlight.fragments();
          String fragmentString = fragments[0].string();
          sourceAsMap.put(entry.getKey(), fragmentString);
        }
        //        LogUtil.info(hit.getScore() + " " + sourceAsMap);
      }
    }
    return searchResult;
  }

  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      if (client != null) {
        client.close();
        LogUtil.info("es client closed");
      }
      sqlManager.close();
    }
  }

  public static void main(String[] args) throws IOException {
    try (ElasticSearchManager manager = new ElasticSearchManager()) {
      manager.indexBatch();
      //      manager.search(new Search("艺术怪力少女数码来袭全场", 0, 30));
      //      manager.search("户外行者小关", 0, 10);
    }
    System.exit(0);
  }
}
