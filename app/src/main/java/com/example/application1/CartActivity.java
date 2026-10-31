package com.example.application1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private static final String DEFAULT_ADDRESS = "湘潭市雨湖区建设北路123号";

    private TextView tvSubtotal, tvDeliveryFee, tvFinalTotal;
    private RecyclerView cartList;
    private final List<CartItem> cartItems = new ArrayList<>();
    private double deliveryFee = 0.0;

    private DataManager.Restaurant restaurant;

    // 内部类：用来组合菜品信息和数量
    static class CartItem {
        DataManager.Dish dish;
        int count;
        int dishIndex; // 菜品在菜单中的位置（从 1 开始），用于定位图片

        CartItem(DataManager.Dish dish, int count, int dishIndex) {
            this.dish = dish;
            this.count = count;
            this.dishIndex = dishIndex;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        cartList = findViewById(R.id.cartList);

        // 1. 接收 MenuActivity 传来的数据
        String restaurantId = getIntent().getStringExtra("restaurant_id");
        HashMap<String, Integer> cartMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("cart_map");

        restaurant = DataManager.findById(restaurantId);
        if (restaurant != null && cartMap != null) {
            deliveryFee = restaurant.deliveryFee;

            // 2. 将 Map 的数据转换成列表给 RecyclerView 用
            for (Map.Entry<String, Integer> entry : cartMap.entrySet()) {
                String dishName = entry.getKey();
                int count = entry.getValue();

                // 在菜单中寻找这道菜的对象（同时记下序号，用于定位菜品图片）
                for (int i = 0; i < restaurant.menu.size(); i++) {
                    DataManager.Dish dish = restaurant.menu.get(i);
                    if (dish.name.equals(dishName)) {
                        cartItems.add(new CartItem(dish, count, i + 1));
                        break;
                    }
                }
            }
        }

        // 3. 设置列表
        cartList.setLayoutManager(new LinearLayoutManager(this));
        cartList.setAdapter(new CartAdapter());

        // 4. 计算总价
        calculateTotal();

        // 5. 支付按钮点击事件：生成订单并落库
        findViewById(R.id.btnPay).setOnClickListener(v -> payAndCreateOrder());
    }

    private double getSubtotal() {
        double subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.dish.price * item.count;
        }
        return subtotal;
    }

    private void calculateTotal() {
        double subtotal = getSubtotal();
        double finalTotal = subtotal + deliveryFee;

        tvSubtotal.setText("¥ " + subtotal);
        tvDeliveryFee.setText("¥ " + deliveryFee);
        tvFinalTotal.setText("订单总价：¥ " + finalTotal);
    }

    /** 支付：把购物车内容转成一笔本地订单，保存后跳到「订单」页 */
    private void payAndCreateOrder() {
        if (cartItems.isEmpty() || restaurant == null) {
            Toast.makeText(this, "购物车为空，无法下单", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = getSubtotal();

        Order order = new Order();
        order.id = generateOrderId();
        order.restaurantId = restaurant.id;
        order.restaurantName = restaurant.name;
        order.address = DEFAULT_ADDRESS;
        order.createTime = System.currentTimeMillis();
        order.subtotal = subtotal;
        order.deliveryFee = deliveryFee;
        order.total = subtotal + deliveryFee;
        order.status = Order.STATUS_PENDING;

        for (CartItem item : cartItems) {
            order.items.add(new Order.OrderItem(item.dish.name, item.dish.price, item.count));
        }

        OrderManager.addOrder(this, order);

        Toast.makeText(this, "支付成功！预计 " + restaurant.deliveryTime + " 分钟送达", Toast.LENGTH_LONG).show();

        // 回到主界面并直接展示「订单」页
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_ORDERS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String generateOrderId() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        int tail = (int) (Math.random() * 9000) + 1000;
        return time + tail;
    }

    // ============ RecyclerView 适配器 ============
    class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cart, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            CartItem item = cartItems.get(position);
            holder.tvName.setText(item.dish.name);
            holder.tvCount.setText("x" + item.count);
            holder.tvPrice.setText("¥ " + (item.dish.price * item.count)); // 显示单品总价

            if (restaurant != null) {
                int imageRes = ImageMapper.dishImage(holder.itemView.getContext(),
                        restaurant.id, item.dishIndex);
                if (imageRes != 0) {
                    holder.ivImage.setImageResource(imageRes);
                }
            }
        }

        @Override
        public int getItemCount() {
            return cartItems.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCount, tvPrice;
            android.widget.ImageView ivImage;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCartItemName);
                tvCount = itemView.findViewById(R.id.tvCartItemCount);
                tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
                ivImage = itemView.findViewById(R.id.ivCartItemImage);
            }
        }
    }
}
