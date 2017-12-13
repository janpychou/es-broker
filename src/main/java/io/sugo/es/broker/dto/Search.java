package io.sugo.es.broker.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Search {
  public static final String TAG_CATEGORY = "tag_category";
  public static final String TAG_SCENE = "tag_scene";
  public static final String TAG_STYLE = "tag_style";
  public static final String TAG_ORIGIN = "tag_origin";

  @JsonProperty
  private String text;
  @JsonProperty
  private List<String> category;
  @JsonProperty
  private List<String> scene;
  @JsonProperty
  private List<String> style;
  @JsonProperty
  private List<String> origin;
  @JsonProperty
  private Integer offset = 0;
  @JsonProperty
  private Integer size = 10;

  //the constructor is only for jackson
  public Search(){}

  public Search(String text, int offset, int size) {
    this.text = text;
    this.offset = offset;
    this.size = size;
  }

  public boolean hasText() {
    return text != null && text.trim().length() > 0;
  }

  public String getText() {
    return text;
  }

  public boolean hasCategory() {
    return category != null && category.size() > 0;
  }

  public List<String> getCategory() {
    return category;
  }

  public boolean hasScene() {
    return scene != null && scene.size() > 0;
  }

  public List<String> getScene() {
    return scene;
  }

  public boolean hasStyle() {
    return style != null && style.size() > 0;
  }

  public List<String> getStyle() {
    return style;
  }

  public boolean hasOrigin() {
    return origin != null && origin.size() > 0;
  }

  public List<String> getOrigin() {
    return origin;
  }

  public int getOffset() {
    return offset;
  }

  public int getSize() {
    if (size == null) {
      size = 10;
    }
    return size;
  }
}
