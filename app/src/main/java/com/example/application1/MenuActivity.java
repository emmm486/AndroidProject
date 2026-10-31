package com.example.application1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuActivity extends AppCompatActivity {

    private List<DataManager.Dish> dishes = new ArrayList<>();
    private final Map<String, Integer> cart = new HashMap<>();
    private TextView tvTotal, tvBadge;
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        String id = getIntent().getStringExtra("restaurant_id");
        restaurantId = id;
        DataManager.Restaurant restaurant = DataManager.findById(id);

        if (restaurant != null) {
            dishes = restaurant.menu;
            ((TextView) findViewById(R.id.tvShopName)).setText(restaurant.name);

            int shopImage = ImageMapper.shopImage(this, id);
            if (shopImage != 0) {
                ((android.widget.ImageView) findViewById(R.id.ivMenuShopImage))
                        .setImageResource(shopImage);
            }

            // 新增：填充商家详细信息卡片
            ((TextView) findViewById(R.id.tvMenuShopInfo)).setText(
                    "⭐ " + restaurant.rating + " | 月售" + restaurant.monthlySales +
                            " | 起送¥" + restaurant.minOrder + " | 配送¥" + restaurant.deliveryFee
            );
            ((TextView) findViewById(R.id.tvMenuShopAddress)).setText("地址：" + restaurant.address);
            ((TextView) findViewById(R.id.tvMenuShopAnnouncement)).setText("公告：" + restaurant.announcement);
        }

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        tvTotal = findViewById(R.id.tvTotal);
        tvBadge = findViewById(R.id.tvBadge);

        // 「再来一单」时由订单页传入的历史菜品数量
        HashMap<String, Integer> prefill = (HashMap<String, Integer>)
                getIntent().getSerializableExtra("prefill_cart_map");
        if (prefill != null && !prefill.isEmpty()) {
            cart.putAll(prefill);
            android.widget.Toast.makeText(this, "已帮你把上次买的菜加回购物车~",
                    android.widget.Toast.LENGTH_SHORT).show();
        }

        RecyclerView list = findViewById(R.id.dishList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new DishAdapter(dishes));
        updateCartBar();

        // 点击购物车或去结算，带上数据跳转
        View.OnClickListener toCartListener = v -> {
            if (cart.isEmpty()) {
                android.widget.Toast.makeText(this, "购物车为空，请先选择商品", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MenuActivity.this, CartActivity.class);
            intent.putExtra("restaurant_id", id);
            // HashMap 实现了 Serializable 接口，可以直接传递
            intent.putExtra("cart_map", new java.util.HashMap<>(cart));
            startActivity(intent);
        };

        findViewById(R.id.cartBar).setOnClickListener(toCartListener);
        findViewById(R.id.btnCheckout).setOnClickListener(toCartListener);
    }

    // ... 购物车 addToCart / removeFromCart / updateCartBar 代码保持你原样不变 ...
    private void addToCart(DataManager.Dish dish) {
        int count = cart.getOrDefault(dish.name, 0);
        cart.put(dish.name, count + 1);
        updateCartBar();
    }

    private void removeFromCart(DataManager.Dish dish) {
        int count = cart.getOrDefault(dish.name, 0);
        if (count <= 1) {
            cart.remove(dish.name);
        } else {
            cart.put(dish.name, count - 1);
        }
        updateCartBar();
    }

    private void updateCartBar() {
        double total = 0;
        int totalCount = 0;
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            for (DataManager.Dish d : dishes) {
                if (d.name.equals(e.getKey())) {
                    total += d.price * e.getValue();
                    break;
                }
            }
            totalCount += e.getValue();
        }
        tvTotal.setText("¥ " + total);
        tvBadge.setText(String.valueOf(totalCount));
    }

    class DishAdapter extends RecyclerView.Adapter<DishAdapter.VH> {

        private final List<DataManager.Dish> data;

        DishAdapter(List<DataManager.Dish> data) {
            this.data = data;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dish, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DataManager.Dish dish = data.get(position);
            holder.tvName.setText(dish.name);
            holder.tvPrice.setText("¥ " + dish.price);

            int dishImage = ImageMapper.dishImage(holder.itemView.getContext(),
                    restaurantId, position + 1);
            if (dishImage != 0) {
                holder.ivDishImage.setImageResource(dishImage);
            }

            // 新增：绑定描述、月售、标签
            holder.tvDesc.setText(dish.desc);
            holder.tvSales.setText("月售 " + dish.monthlySales);
            if (dish.tags != null && !dish.tags.isEmpty()) {
                // 用简单的字符串拼接展示标签，例如: [招牌] [热卖]
                StringBuilder tagsStr = new StringBuilder();
                for (String tag : dish.tags) {
                    tagsStr.append("[").append(tag).append("] ");
                }
                holder.tvTags.setText(tagsStr.toString().trim());
                holder.tvTags.setVisibility(View.VISIBLE);
            } else {
                holder.tvTags.setVisibility(View.GONE);
            }

            int count = cart.getOrDefault(dish.name, 0);
            refreshRow(holder, count);

            holder.btnAdd.setOnClickListener(v -> {
                addToCart(dish);
                refreshRow(holder, cart.getOrDefault(dish.name, 0));
            });

            holder.btnMinus.setOnClickListener(v -> {
                removeFromCart(dish);
                refreshRow(holder, cart.getOrDefault(dish.name, 0));
            });
        }

        private void refreshRow(VH holder, int count) {
            if (count > 0) {
                holder.tvCount.setText(String.valueOf(count));
                holder.btnMinus.setVisibility(View.VISIBLE);
                holder.tvCount.setVisibility(View.VISIBLE);
            } else {
                holder.btnMinus.setVisibility(View.GONE);
                holder.tvCount.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            // 新增 tvTags, tvDesc, tvSales
            TextView tvName, tvPrice, btnAdd, btnMinus, tvCount, tvTags, tvDesc, tvSales;
            android.widget.ImageView ivDishImage;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvDishName);
                tvPrice = itemView.findViewById(R.id.tvDishPrice);
                btnAdd = itemView.findViewById(R.id.btnAdd);
                btnMinus = itemView.findViewById(R.id.btnMinus);
                tvCount = itemView.findViewById(R.id.tvCount);
                tvTags = itemView.findViewById(R.id.tvDishTags);
                tvDesc = itemView.findViewById(R.id.tvDishDesc);
                tvSales = itemView.findViewById(R.id.tvDishSales);
                ivDishImage = itemView.findViewById(R.id.ivDishImage);
            }
        }
    }
}