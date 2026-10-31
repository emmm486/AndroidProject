package com.example.application1;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单数据模型。
 * 一条已支付的外卖订单包含：商家、商品明细、金额、收货地址、状态与时间。
 */
public class Order {

    /** 已支付，等待商家接单 */
    public static final int STATUS_PENDING = 0;
    /** 商家已接单，正在备餐 */
    public static final int STATUS_ACCEPTED = 1;
    /** 骑手已取餐，配送途中 */
    public static final int STATUS_DELIVERING = 2;
    /** 已送达，待评价 */
    public static final int STATUS_DELIVERED = 3;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 4;

    public String id;
    public String restaurantId;
    public String restaurantName;
    public String address;
    public String remark = "";
    public long createTime;

    public double subtotal;
    public double deliveryFee;
    public double total;

    public int status;
    public boolean rated;
    /** true 表示状态由用户手动置位（如提前确认收货），不再随时间自动推进 */
    public boolean statusLocked;

    public List<OrderItem> items = new ArrayList<>();

    public static class OrderItem {
        public String name;
        public double price;
        public int count;

        public OrderItem() {
        }

        public OrderItem(String name, double price, int count) {
            this.name = name;
            this.price = price;
            this.count = count;
        }
    }

    public Order() {
    }

    public int getTotalCount() {
        int sum = 0;
        for (OrderItem item : items) {
            sum += item.count;
        }
        return sum;
    }

    /** 菜品摘要，例如「黄焖鸡米饭（大份）等 3 件商品」 */
    public String getDishSummary() {
        if (items.isEmpty()) return "暂无商品";
        String first = items.get(0).name;
        int total = getTotalCount();
        if (items.size() == 1 && total == 1) return first;
        return first + " 等 " + total + " 件商品";
    }

    /** 全部菜品名拼接，例如「红烧肉 x1，米饭 x2」 */
    public String getItemDetail() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            if (i > 0) sb.append("，");
            sb.append(item.name).append(" x").append(item.count);
        }
        return sb.toString();
    }

    /** 是否还在进行中（未送达、未取消） */
    public boolean isOngoing() {
        return status == STATUS_PENDING || status == STATUS_ACCEPTED || status == STATUS_DELIVERING;
    }

    public static String statusText(int status) {
        switch (status) {
            case STATUS_PENDING:
                return "待接单";
            case STATUS_ACCEPTED:
                return "商家备餐中";
            case STATUS_DELIVERING:
                return "骑手配送中";
            case STATUS_DELIVERED:
                return "已送达";
            case STATUS_CANCELLED:
                return "已取消";
            default:
                return "未知状态";
        }
    }

    /** 不同状态对应的提示文案 */
    public static String statusHint(int status) {
        switch (status) {
            case STATUS_PENDING:
                return "订单已支付，正在等待商家接单";
            case STATUS_ACCEPTED:
                return "商家已接单，正在为你现做";
            case STATUS_DELIVERING:
                return "骑手正在飞奔而来，请耐心等待";
            case STATUS_DELIVERED:
                return "订单已完成，期待你的评价";
            case STATUS_CANCELLED:
                return "该订单已取消，款项将原路退回";
            default:
                return "";
        }
    }
}
