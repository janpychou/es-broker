package io.sugo.es.broker;

import io.sugo.utils.LogUtil;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class SqlManager implements Closeable{

  private String url;
  private String username;
  private String password;
  private String table;
  private String columns;
  private Set<String> tagColumns;
  private Map<String, Set<String>> map = new HashMap<>();

  public SqlManager() {
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      LogUtil.error("", e);
    }
    url = SystemConfig.getString(SystemConfig.MYSQL_URL);
    username = SystemConfig.getString(SystemConfig.MYSQL_USERNAME);
    password = SystemConfig.getString(SystemConfig.MYSQL_PASSWORD);
    table = SystemConfig.getString(SystemConfig.MYSQL_TABLE);
    columns = SystemConfig.getString(SystemConfig.MYSQL_COLUMNS);
    String tagColumnStr = SystemConfig.getString(SystemConfig.MYSQL_TAG_COLUMNS);
    tagColumns = new HashSet<>(Arrays.asList(tagColumnStr.split(",")));
  }

  public List<Map<String, Object>> readPaging(int offset, int limit) {
    List<Map<String, Object>> jsonMaps = new ArrayList<>(limit);
    Connection conn = null;
    try {
      conn = DriverManager.getConnection(url, username, password);
      Statement stmt = conn.createStatement();

      String sql = String.format("select %s from %s LIMIT %d OFFSET %d ", columns, table, limit, offset);
      //      LogUtil.info(sql);
      ResultSet rs = stmt.executeQuery(sql);
      ResultSetMetaData meta = rs.getMetaData();
      int colCnt = meta.getColumnCount() + 1;
      int cnt = 0;

      while (rs.next()) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMaps.add(jsonMap);
        for (int i = 1; i < colCnt; i++) {
          String colName = meta.getColumnLabel(i);
          if (tagColumns.contains(colName)) {
            String val = rs.getString(i);
            if (val != null) {
              String[] items;
              if (val.contains(",")) {
                items = val.split(",");
                jsonMap.put(colName, items);
              } else if (val.contains(" ")) {
                items = val.split(" ");
                jsonMap.put(colName, items);
              } else {
                jsonMap.put(colName, val);
                items = new String[]{val};
              }
              if(items != null) {
                Set<String> set = map.get(colName);
                if(set == null) {
                  set = new HashSet<>();
                  map.put(colName, set);
                }
                set.addAll(Arrays.asList(items));
              }
            }
            continue;
          }
          int type = meta.getColumnType(i);
          switch (type) {
          case Types.VARCHAR:
          case Types.CHAR:
            String val = rs.getString(i);
            if (val != null) {
              jsonMap.put(colName, val);
            }
            break;
          case Types.INTEGER:
            jsonMap.put(colName, rs.getInt(i));
            break;
          case Types.TIMESTAMP:
            jsonMap.put(colName, rs.getTimestamp(i).getTime());
            break;
          default:
            throw new RuntimeException(String.format("unsupported column:%s:%s", colName, meta.getColumnTypeName(i)));
          }
        }
        cnt++;
        //        translateCategory(jsonMap);
        //        LogUtil.formatInfo("%s", jsonMap);
      }
      //      LogUtil.formatInfo("read data rows:%d", cnt);
      rs.close();
      stmt.close();
    } catch (SQLException e) {
      LogUtil.error(String.format("connect mysql[%s] error", url), e);
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
        }
      }
    }
    return jsonMaps;
  }

  private void translateCategory(Map<String, Object> jsonMap) {
    String module = (String) jsonMap.get("module");
    String category = (String) jsonMap.get("tag_category");
    if ("淘宝头条".equals(module)) {
      switch (category) {
      case "美搭":
        jsonMap.put("tag_category", "女装");
        break;
      case "型男":
        jsonMap.put("tag_category", "男装");
        break;
      case "数码":
        jsonMap.put("tag_category", "科技");
        break;
      case "运动":
        jsonMap.put("tag_category", "运动");
        break;
      case "美容":
        jsonMap.put("tag_category", "美妆");
        break;
      case "母婴":
        jsonMap.put("tag_category", "母婴");
        break;
      case "瘦身健康":
        jsonMap.put("tag_category", "健康");
        break;
      case "手作":
        jsonMap.put("tag_category", "设计");
        break;
      case "趣玩":
        jsonMap.put("tag_category", "趣玩");
        break;
      case "萌宠":
        jsonMap.put("tag_category", "宠物");
        break;
      case "美食":
        jsonMap.put("tag_category", "食品");
        break;
      case "居家":
        jsonMap.put("tag_category", "家居");
        break;
      }
    } else if ("必买清单".equals(module)) {
      switch (category) {
      case "穿搭":
        jsonMap.put("tag_category", "女装");
        break;
      case "耍帅":
        jsonMap.put("tag_category", "男装");
        break;
      case "潮玩":
        jsonMap.put("tag_category", "科技");
        break;
      case "运动":
        jsonMap.put("tag_category", "运动");
        break;
      case "化妆":
        jsonMap.put("tag_category", "美妆");
        break;
      case "育儿":
        jsonMap.put("tag_category", "母婴");
        break;
      case "瘦身健康":
        jsonMap.put("tag_category", "健康");
        break;
      case "手作":
        jsonMap.put("tag_category", "设计");
        break;
      case "趣玩":
        jsonMap.put("tag_category", "趣玩");
        break;
      case "萌宠":
        jsonMap.put("tag_category", "宠物");
        break;
      case "爱吃":
        jsonMap.put("tag_category", "食品");
        break;
      case "置家":
        jsonMap.put("tag_category", "家居");
        break;
      }
    }

    String[] scenes = new String[]{"礼物", "家庭", "恋爱", "海淘", "工作", "休息", "假日", "天气"};
    String[] styles = new String[]{"品质", "个性", "热门", "优惠", "新潮", "专业", "文艺", "奢侈"};
    String[] sources = new String[]{"淘宝头条", "必买清单", "严选"};
    List<String> list = new ArrayList<>();
    Random rand = new Random();
    list.add(sources[rand.nextInt(sources.length)]);

    int len = rand.nextInt(5);
    for (int i = 0; i < len; i++) {
      int next = rand.nextInt(2);
      if (next == 0) {
        list.add(scenes[rand.nextInt(scenes.length)]);
      } else {
        list.add(styles[rand.nextInt(styles.length)]);
      }
    }
    jsonMap.put("category", list);
    jsonMap.put("tag_scene", scenes[rand.nextInt(scenes.length)]);
    jsonMap.put("tag_style", styles[rand.nextInt(styles.length)]);
    jsonMap.put("tag_source", sources[rand.nextInt(sources.length)]);
  }

  public static void main(String[] args) throws ClassNotFoundException {
    SqlManager sqlManager = new SqlManager();
    List<Map<String, Object>> hls = sqlManager.readPaging(0, 100);
    LogUtil.info(hls.size());
  }

  @Override
  public void close() throws IOException {
    LogUtil.info(map);
  }
}
