package com.example.application1;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

    // ===== 数据模型：对应 JSON 里的结构 =====
    public static class Dish {
        public String name;
        public double price;
        public int monthlySales;
        public String desc;
        public List<String> tags = new ArrayList<>(); // 新增：标签
    }

    public static class Restaurant {
        public String id;
        public String name;
        public String category;
        public double rating;
        public int monthlySales;
        public double minOrder;
        public double deliveryFee;
        public int deliveryTime;
        public double distance;       // 新增：距离
        public String address;        // 新增：地址
        public String announcement;   // 新增：公告
        public List<Dish> menu = new ArrayList<>();
    }

    private static List<Restaurant> restaurants;

    public static List<Restaurant> load(Context context) {
        if (restaurants != null) return restaurants;

        restaurants = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("takeout_data.json");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.getJSONArray("restaurants");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject r = arr.getJSONObject(i);
                Restaurant rest = new Restaurant();
                rest.id = r.getString("id");
                rest.name = r.getString("name");
                rest.category = r.getString("category");
                rest.rating = r.getDouble("rating");
                rest.monthlySales = r.getInt("monthly_sales");
                rest.minOrder = r.getDouble("min_order");
                rest.deliveryFee = r.getDouble("delivery_fee");
                rest.deliveryTime = r.getInt("delivery_time_min");
                // 新增字段解析
                rest.distance = r.optDouble("distance_km", 0.0);
                rest.address = r.optString("address", "暂无地址");
                rest.announcement = r.optString("announcement", "暂无公告");

                JSONArray menuArr = r.getJSONArray("menu");
                for (int j = 0; j < menuArr.length(); j++) {
                    JSONObject m = menuArr.getJSONObject(j);
                    Dish d = new Dish();
                    d.name = m.getString("name");
                    d.price = m.getDouble("price");
                    d.monthlySales = m.getInt("monthly_sales");
                    d.desc = m.optString("desc", "");

                    // 解析 tags 数组
                    JSONArray tagsArr = m.optJSONArray("tags");
                    if (tagsArr != null) {
                        for (int k = 0; k < tagsArr.length(); k++) {
                            d.tags.add(tagsArr.getString(k));
                        }
                    }
                    rest.menu.add(d);
                }
                restaurants.add(rest);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return restaurants;
    }

    public static Restaurant findById(String id) {
        if (restaurants == null) return null;
        for (Restaurant r : restaurants) {
            if (r.id.equals(id)) return r;
        }
        return null;
    }
}