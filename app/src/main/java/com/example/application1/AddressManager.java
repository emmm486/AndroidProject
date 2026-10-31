package com.example.application1;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 收货地址仓库：SharedPreferences + JSON 持久化，重启 App 不丢。
 * 首次运行写入两条示例地址。
 */
public class AddressManager {

    private static final String PREF_NAME = "takeout_addresses";
    private static final String KEY_LIST = "address_list";

    private static SharedPreferences pref(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 读取全部地址（默认地址排在最前） */
    public static List<Address> getAddresses(Context context) {
        String raw = pref(context).getString(KEY_LIST, null);
        if (raw == null) {
            List<Address> seeds = seedDefaults();
            persist(context, seeds);
            return seeds;
        }
        List<Address> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                list.add(new Address(
                        json.optString("id"),
                        json.optString("name"),
                        json.optString("phone"),
                        json.optString("detail"),
                        json.optBoolean("isDefault", false)));
            }
        } catch (JSONException e) {
            list = seedDefaults();
            persist(context, list);
        }
        return list;
    }

    private static List<Address> seedDefaults() {
        List<Address> list = new ArrayList<>();
        list.add(new Address(newId(), "小明", "13888888888",
                "湘潭市雨湖区建设北路123号 3栋502室", true));
        list.add(new Address(newId(), "小明", "13888888888",
                "湘潭大学琴湖公寓 6栋208室", false));
        return list;
    }

    /** 新增地址，返回新地址 id */
    public static String add(Context context, String name, String phone,
                             String detail, boolean isDefault) {
        List<Address> list = getAddresses(context);
        if (isDefault) {
            for (Address a : list) a.isDefault = false;
        } else if (list.isEmpty()) {
            // 第一条地址强制为默认
            isDefault = true;
        }
        Address address = new Address(newId(), name, phone, detail, isDefault);
        list.add(address);
        persist(context, list);
        return address.id;
    }

    public static void update(Context context, Address updated) {
        List<Address> list = getAddresses(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(updated.id)) {
                if (updated.isDefault) {
                    for (Address a : list) a.isDefault = false;
                }
                list.set(i, updated);
                persist(context, list);
                return;
            }
        }
    }

    public static void delete(Context context, String id) {
        List<Address> list = getAddresses(context);
        Address removed = null;
        for (Address a : list) {
            if (a.id.equals(id)) {
                removed = a;
                break;
            }
        }
        if (removed == null) return;
        list.remove(removed);
        // 删掉的是默认地址且还有其他地址 → 让第一个顶上
        if (removed.isDefault && !list.isEmpty()) {
            list.get(0).isDefault = true;
        }
        persist(context, list);
    }

    public static void setDefault(Context context, String id) {
        List<Address> list = getAddresses(context);
        for (Address a : list) {
            a.isDefault = a.id.equals(id);
        }
        persist(context, list);
    }

    public static int count(Context context) {
        return getAddresses(context).size();
    }

    private static String newId() {
        return "addr_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static void persist(Context context, List<Address> list) {
        JSONArray array = new JSONArray();
        for (Address a : list) {
            JSONObject json = new JSONObject();
            try {
                json.put("id", a.id);
                json.put("name", a.name);
                json.put("phone", a.phone);
                json.put("detail", a.detail);
                json.put("isDefault", a.isDefault);
                array.put(json);
            } catch (JSONException ignored) {
            }
        }
        pref(context).edit().putString(KEY_LIST, array.toString()).apply();
    }
}
