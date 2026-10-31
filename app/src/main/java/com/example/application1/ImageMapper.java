package com.example.application1;

import android.content.Context;

/**
 * 图片资源定位：按约定把店铺 / 菜品映射到 drawable-nodpi 下的图片。
 * 店铺：shop_<店铺id>.png        例：shop_r001
 * 菜品：dish_<店铺id>_<序号>.png  例：dish_r001_01（序号为菜品在菜单中的位置，从 1 开始）
 */
public class ImageMapper {

    public static int shopImage(Context context, String restaurantId) {
        return find(context, "shop_" + restaurantId);
    }

    public static int dishImage(Context context, String restaurantId, int dishIndex) {
        return find(context, String.format("dish_%s_%02d", restaurantId, dishIndex));
    }

    private static int find(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }
}
