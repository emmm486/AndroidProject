package com.example.application1;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 个人中心的数据仓库：昵称、手机号、余额、优惠券、积分、收藏。
 * 本地持久化到 SharedPreferences，重启 App 后仍然保留。
 */
public class UserManager {

    private static final String PREF_NAME = "takeout_user";

    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_COUPONS = "coupons";
    private static final String KEY_POINTS = "points";
    private static final String KEY_FAVORITES = "favorites";

    private static SharedPreferences pref(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getNickname(Context c) {
        return pref(c).getString(KEY_NICKNAME, "外卖小达人");
    }

    public static void setNickname(Context c, String value) {
        pref(c).edit().putString(KEY_NICKNAME, value).apply();
    }

    public static String getPhone(Context c) {
        return pref(c).getString(KEY_PHONE, "13888888888");
    }

    public static void setPhone(Context c, String value) {
        pref(c).edit().putString(KEY_PHONE, value).apply();
    }

    public static String getMaskedPhone(Context c) {
        String phone = getPhone(c);
        if (phone.length() == 11 && phone.matches("\\d{11}")) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }

    public static double getBalance(Context c) {
        return Double.parseDouble(pref(c).getString(KEY_BALANCE, "12.50"));
    }

    public static void addBalance(Context c, double delta) {
        pref(c).edit().putString(KEY_BALANCE, String.valueOf(getBalance(c) + delta)).apply();
    }

    public static int getCoupons(Context c) {
        return pref(c).getInt(KEY_COUPONS, 3);
    }

    public static void addCoupons(Context c, int delta) {
        pref(c).edit().putInt(KEY_COUPONS, Math.max(0, getCoupons(c) + delta)).apply();
    }

    public static int getPoints(Context c) {
        return pref(c).getInt(KEY_POINTS, 128);
    }

    public static void addPoints(Context c, int delta) {
        pref(c).edit().putInt(KEY_POINTS, Math.max(0, getPoints(c) + delta)).apply();
    }

    public static int getFavorites(Context c) {
        return pref(c).getInt(KEY_FAVORITES, 6);
    }

    public static void addFavorites(Context c, int delta) {
        pref(c).edit().putInt(KEY_FAVORITES, Math.max(0, getFavorites(c) + delta)).apply();
    }

    /** 由积分换算出的会员等级 */
    public static String getMemberLevel(Context c) {
        int points = getPoints(c);
        if (points >= 2000) return "钻石会员";
        if (points >= 1000) return "黑金会员";
        if (points >= 500) return "黄金会员";
        if (points >= 200) return "白银会员";
        return "普通会员";
    }
}
