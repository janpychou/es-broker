package io.sugo.es.broker.dto;

import org.joda.time.DateTime;

public class HeadLine {
  private String id;
  private String title;
  private String website;
  private String module;
  private String category;
  private int siteHotCount;
  private String author;
  private DateTime createdAt;
  private DateTime updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public int getSiteHotCount() {
    return siteHotCount;
  }

  public void setSiteHotCount(int siteHotCount) {
    this.siteHotCount = siteHotCount;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public DateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = new DateTime(createdAt);
  }

  public DateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = new DateTime(updatedAt);
  }
}
