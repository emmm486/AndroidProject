package com.example.application1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 「点餐」页：商家列表 + 搜索 + 分类筛选。
 */
public class HomeFragment extends Fragment {

    private List<DataManager.Restaurant> allRestaurants;
    private RestaurantAdapter adapter;
    private EditText etSearch;
    private View emptyView;

    private String currentCategory = "";
    private String currentKeyword = "";

    private final TextView[] categoryViews = new TextView[5];
    private final String[] categoryNames = {"美食", "汉堡披萨", "奶茶饮品", "轻食沙拉", "甜品蛋糕"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allRestaurants = DataManager.load(requireContext());

        RecyclerView list = view.findViewById(R.id.restaurantList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RestaurantAdapter(new ArrayList<>(allRestaurants));
        list.setAdapter(adapter);

        emptyView = view.findViewById(R.id.emptyView);

        etSearch = view.findViewById(R.id.etSearch);
        setupSearch();
        setupCategories(view);
        updateCategoryUI();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentKeyword = s.toString().trim();
                applyFilters();
            }
        });
    }

    private void setupCategories(View root) {
        categoryViews[0] = root.findViewById(R.id.tvCat1);
        categoryViews[1] = root.findViewById(R.id.tvCat2);
        categoryViews[2] = root.findViewById(R.id.tvCat3);
        categoryViews[3] = root.findViewById(R.id.tvCat4);
        categoryViews[4] = root.findViewById(R.id.tvCat5);

        for (int i = 0; i < categoryViews.length; i++) {
            String categoryName = categoryNames[i];
            categoryViews[i].setOnClickListener(v -> {
                // 再次点击已选中的分类 -> 取消选择，回到"全部"
                if (currentCategory.equals(categoryName)) {
                    currentCategory = "";
                } else {
                    currentCategory = categoryName;
                }
                updateCategoryUI();
                applyFilters();
            });
        }
    }

    private void updateCategoryUI() {
        if (getContext() == null) return;
        int selectedColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);
        int normalColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray);

        for (int i = 0; i < categoryViews.length; i++) {
            if (categoryNames[i].equals(currentCategory)) {
                categoryViews[i].setTextColor(selectedColor);
            } else {
                categoryViews[i].setTextColor(normalColor);
            }
        }
    }

    private void applyFilters() {
        List<DataManager.Restaurant> filteredList = new ArrayList<>();

        for (DataManager.Restaurant r : allRestaurants) {
            boolean matchCategory = currentCategory.isEmpty()
                    || (r.category != null && r.category.contains(currentCategory));
            boolean matchKeyword = currentKeyword.isEmpty() || checkKeywordMatch(r, currentKeyword);

            if (matchCategory && matchKeyword) {
                filteredList.add(r);
            }
        }

        adapter.updateData(filteredList);
        if (emptyView != null) {
            emptyView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean checkKeywordMatch(DataManager.Restaurant r, String keyword) {
        if (r.name.contains(keyword)) return true;
        for (DataManager.Dish dish : r.menu) {
            if (dish.name.contains(keyword)) return true;
        }
        return false;
    }

    // ================== RecyclerView Adapter ==================
    class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.VH> {

        private List<DataManager.Restaurant> data;

        RestaurantAdapter(List<DataManager.Restaurant> data) {
            this.data = data;
        }

        void updateData(List<DataManager.Restaurant> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_restaurant, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DataManager.Restaurant r = data.get(position);
            holder.tvName.setText(r.name);
            holder.tvRating.setText("⭐ " + r.rating + "  月售" + r.monthlySales + "  距离" + r.distance + "km");
            holder.tvDelivery.setText("起送¥" + r.minOrder
                    + " | 配送¥" + r.deliveryFee + " | " + r.deliveryTime + "分钟");
            holder.tvAddress.setText(r.address);
            holder.tvAnnouncement.setText("公告：" + r.announcement);

            int imageRes = ImageMapper.shopImage(holder.itemView.getContext(), r.id);
            if (imageRes != 0) {
                holder.ivShopImage.setImageResource(imageRes);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MenuActivity.class);
                intent.putExtra("restaurant_id", r.id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRating, tvDelivery, tvAddress, tvAnnouncement;
            android.widget.ImageView ivShopImage;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvRating = itemView.findViewById(R.id.tvRating);
                tvDelivery = itemView.findViewById(R.id.tvDelivery);
                tvAddress = itemView.findViewById(R.id.tvAddress);
                tvAnnouncement = itemView.findViewById(R.id.tvAnnouncement);
                ivShopImage = itemView.findViewById(R.id.ivShopImage);
            }
        }
    }
}
