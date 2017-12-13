package io.sugo.es.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class IndexResult {
  @JsonProperty
  private String indexName;
  @JsonProperty
  private String typeName;
  @JsonProperty
  private String idColumn;
  @JsonProperty
  private int rowCount;
  @JsonProperty
  private int bachCount;
  @JsonProperty
  private long spendTime;

  private DateTime start;
  private DateTime end;

  public IndexResult(String indexName, String typeName, String idColumn) {
    this.indexName = indexName;
    this.typeName = typeName;
    this.idColumn = idColumn;
    start = new DateTime();
  }

  public void increaseBach() {
    this.bachCount++;
  }

  public long getSpendTime() {
    if (end == null) {
      end = new DateTime();
    }
    this.spendTime = end.getMillis() - start.getMillis();
    return this.spendTime;
  }

  public void finish(int rowCount) {
    this.rowCount = rowCount;
    end = new DateTime();
  }
}
