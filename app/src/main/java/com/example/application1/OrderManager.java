package com.example.application1;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 订单仓库：负责订单的本地持久化（SharedPreferences）与状态流转。
 *
 * 说明：本项目没有后端，订单状态按下单后经过的时间自动推进，用于演示：
 *   0 ~ 8s   待接单
 *   8s ~ 20s  商家备餐中
 *   20s ~ 40s 骑手配送中
 *   40s 之后  已送达（整个演示过程约 40 秒）
 */
public class OrderManager {

    private static final String PREF_NAME = "takeout_orders";
    private static final String KEY_ORDERS = "orders_json";

    private static final long T_ACCEPTED = 8 * 1000L;
    private static final long T_DELIVERING = 20 * 1000L;
    private static final long T_DELIVERED = 40 * 1000L;

    private static List<Order> cache;

    public static List<Order> getOrders(Context context) {
        load(context);
        refreshStatus(context);
        return cache;
    }

    /** 只看进行中的订单 */
    public static List<Order> getOngoingOrders(Context context) {
        List<Order> result = new ArrayList<>();
        for (Order o : getOrders(context)) {
            if (o.isOngoing()) result.add(o);
        }
        return result;
    }

    private static void load(Context context) {
        if (cache != null) return;
        cache = new ArrayList<>();

        SharedPreferences pref = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(KEY_ORDERS, "");
        if (json == null || json.isEmpty()) return;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Order o = fromJson(arr.getJSONObject(i));
                if (o != null) cache.add(o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sortByTimeDesc();
    }

    /** 根据下单时间推进订单状态（已取消的订单不再推进） */
    private static void refreshStatus(Context context) {
        boolean changed = false;
        long now = System.currentTimeMillis();

        for (Order o : cache) {
            if (o.status == Order.STATUS_CANCELLED || o.statusLocked) continue;

            long elapsed = now - o.createTime;
            int nextStatus;
            if (elapsed >= T_DELIVERED) {
                nextStatus = Order.STATUS_DELIVERED;
            } else if (elapsed >= T_DELIVERING) {
                nextStatus = Order.STATUS_DELIVERING;
            } else if (elapsed >= T_ACCEPTED) {
                nextStatus = Order.STATUS_ACCEPTED;
            } else {
                nextStatus = Order.STATUS_PENDING;
            }

            if (o.status != nextStatus) {
                o.status = nextStatus;
                changed = true;
            }
        }

        if (changed) persist(context);
    }

    public static void addOrder(Context context, Order order) {
        load(context);
        cache.add(0, order);
        persist(context);
    }

    public static Order findById(Context context, String id) {
        if (id == null) return null;
        for (Order o : getOrders(context)) {
            if (id.equals(o.id)) return o;
        }
        return null;
    }

    public static void cancelOrder(Context context, String id) {
        Order o = findById(context, id);
        if (o == null) return;
        o.status = Order.STATUS_CANCELLED;
        persist(context);
    }

    public static void markRated(Context context, String id) {
        Order o = findById(context, id);
        if (o == null) return;
        o.rated = true;
        persist(context);
    }

    /** 用户提前确认收货：锁定状态，不再随时间自动推进 */
    public static void confirmReceived(Context context, String id) {
        Order o = findById(context, id);
        if (o == null) return;
        o.status = Order.STATUS_DELIVERED;
        o.statusLocked = true;
        persist(context);
    }

    public static void deleteOrder(Context context, String id) {
        Order target = findById(context, id);
        if (target == null) return;
        cache.remove(target);
        persist(context);
    }

    public static void clearAll(Context context) {
        load(context);
        cache.clear();
        persist(context);
    }

    private static void persist(Context context) {
        if (cache == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (Order o : cache) {
                arr.put(toJson(o));
            }
            context.getApplicationContext()
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ORDERS, arr.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sortByTimeDesc() {
        Collections.sort(cache, new Comparator<Order>() {
            @Override
            public int compare(Order a, Order b) {
                return Long.compare(b.createTime, a.createTime);
            }
        });
    }

    // ================== JSON 序列化 ==================

    private static JSONObject toJson(Order o) throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", o.id);
        json.put("restaurantId", o.restaurantId);
        json.put("restaurantName", o.restaurantName);
        json.put("address", o.address);
        json.put("remark", o.remark);
        json.put("createTime", o.createTime);
        json.put("subtotal", o.subtotal);
        json.put("deliveryFee", o.deliveryFee);
        json.put("total", o.total);
        json.put("status", o.status);
        json.put("rated", o.rated);
        json.put("statusLocked", o.statusLocked);

        JSONArray items = new JSONArray();
        for (Order.OrderItem item : o.items) {
            JSONObject ji = new JSONObject();
            ji.put("name", item.name);
            ji.put("price", item.price);
            ji.put("count", item.count);
            items.put(ji);
        }
        json.put("items", items);
        return json;
    }

    private static Order fromJson(JSONObject json) {
        try {
            Order o = new Order();
            o.id = json.optString("id", "");
            o.restaurantId = json.optString("restaurantId", "");
            o.restaurantName = json.optString("restaurantName", "");
            o.address = json.optString("address", "");
            o.remark = json.optString("remark", "");
            o.createTime = json.optLong("createTime", System.currentTimeMillis());
            o.subtotal = json.optDouble("subtotal", 0.0);
            o.deliveryFee = json.optDouble("deliveryFee", 0.0);
            o.total = json.optDouble("total", 0.0);
            o.status = json.optInt("status", Order.STATUS_PENDING);
            o.rated = json.optBoolean("rated", false);
            o.statusLocked = json.optBoolean("statusLocked", false);

            JSONArray items = json.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject ji = items.getJSONObject(i);
                    Order.OrderItem item = new Order.OrderItem();
                    item.name = ji.optString("name", "");
                    item.price = ji.optDouble("price", 0.0);
                    item.count = ji.optInt("count", 1);
                    o.items.add(item);
                }
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
