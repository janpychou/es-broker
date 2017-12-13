package io.sugo.es.broker.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchResult {
//  @JsonProperty
//  private int status;
  @JsonProperty
  private long spend;
//  @JsonProperty
//  private boolean terminated;
//  @JsonProperty
//  private boolean timeout;
  @JsonProperty
  private long totalHits;
  @JsonProperty
  private float maxScore;
//  @JsonProperty
//  private int totalShards;
//  @JsonProperty
//  private int successfulShards;
//  @JsonProperty
//  private int failedShards;
//  @JsonProperty
//  private String failure;
  @JsonProperty
  private List<Map<String, Object>> sources;

  public SearchResult() {
    sources = new ArrayList<>();
  }

//  public int getStatus() {
//    return status;
//  }
//
//  public void setStatus(int status) {
//    this.status = status;
//  }

  public long getSpend() {
    return spend;
  }

  public void setSpend(long spend) {
    this.spend = spend;
  }
//
//  public boolean isTerminated() {
//    return terminated;
//  }
//
//  public void setTerminated(boolean terminated) {
//    this.terminated = terminated;
//  }
//
//  public boolean isTimeout() {
//    return timeout;
//  }
//
//  public void setTimeout(boolean timeout) {
//    this.timeout = timeout;
//  }

  public long getTotalHits() {
    return totalHits;
  }

  public void setTotalHits(long totalHits) {
    this.totalHits = totalHits;
  }

  public float getMaxScore() {
    return maxScore;
  }

  public void setMaxScore(float maxScore) {
    this.maxScore = maxScore;
  }
//
//  public int getTotalShards() {
//    return totalShards;
//  }
//
//  public void setTotalShards(int totalShards) {
//    this.totalShards = totalShards;
//  }
//
//  public int getSuccessfulShards() {
//    return successfulShards;
//  }
//
//  public void setSuccessfulShards(int successfulShards) {
//    this.successfulShards = successfulShards;
//  }
//
//  public int getFailedShards() {
//    return failedShards;
//  }
//
//  public void setFailedShards(int failedShards) {
//    this.failedShards = failedShards;
//  }
//
//  public String getFailure() {
//    return failure;
//  }
//
//  public void setFailure(String failure) {
//    this.failure = failure;
//  }

  public void addSource(Map<String, Object> source) {
    sources.add(source);
  }

  public List<Map<String, Object>> getSources() {
    return sources;
  }
}
