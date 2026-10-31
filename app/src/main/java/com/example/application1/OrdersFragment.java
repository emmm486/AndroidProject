package com.example.application1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * 「订单」页：查看已下单的外卖，支持按状态筛选、取消、再来一单、评价、删除。
 */
public class OrdersFragment extends Fragment {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_ONGOING = 1;
    public static final int FILTER_HISTORY = 2;

    private RecyclerView orderList;
    private View emptyView;
    private TextView tvOngoingCount;
    private TextView tabAll, tabOngoing, tabHistory;

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayed = new ArrayList<>();
    private OrderAdapter adapter;
    private int currentFilter = FILTER_ALL;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (getActivity() instanceof MainActivity) {
                String tab = ((MainActivity) getActivity()).getCurrentTab();
                if (MainActivity.TAB_ORDERS.equals(tab)) {
                    refreshOrders();
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        orderList = view.findViewById(R.id.orderList);
        emptyView = view.findViewById(R.id.emptyView);
        tvOngoingCount = view.findViewById(R.id.tvOngoingCount);
        tabAll = view.findViewById(R.id.tabAll);
        tabOngoing = view.findViewById(R.id.tabOngoing);
        tabHistory = view.findViewById(R.id.tabHistory);

        orderList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderAdapter();
        orderList.setAdapter(adapter);

        setupFilterTabs();

        view.findViewById(R.id.btnGoOrder).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showTab(MainActivity.TAB_HOME);
            }
        });

        refreshOrders();
    }

    private void setupFilterTabs() {
        tabAll.setOnClickListener(v -> switchFilter(FILTER_ALL));
        tabOngoing.setOnClickListener(v -> switchFilter(FILTER_ONGOING));
        tabHistory.setOnClickListener(v -> switchFilter(FILTER_HISTORY));
    }

    private void switchFilter(int filter) {
        currentFilter = filter;
        updateFilterTabs();
        refreshOrders();
    }

    /** 供外部（如「我的」页快捷入口）切换到指定筛选 */
    public void switchToFilter(int filter) {
        switchFilter(filter);
    }

    private void updateFilterTabs() {
        if (getContext() == null) return;
        int selected = ContextCompat.getColor(requireContext(), R.color.white);
        int normal = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        setTabStyle(tabAll, currentFilter == FILTER_ALL, selected, normal);
        setTabStyle(tabOngoing, currentFilter == FILTER_ONGOING, selected, normal);
        setTabStyle(tabHistory, currentFilter == FILTER_HISTORY, selected, normal);
    }

    private void setTabStyle(TextView tv, boolean isSelected, int selectedColor, int normalColor) {
        tv.setTextColor(isSelected ? selectedColor : normalColor);
        tv.setBackgroundResource(isSelected
                ? R.drawable.bg_tab_pill_selected
                : R.drawable.bg_tab_pill_normal);
    }

    /** 从本地重新读取订单并刷新列表（可随时安全调用） */
    public void refreshOrders() {
        if (getView() == null || getContext() == null) return;

        allOrders.clear();
        allOrders.addAll(OrderManager.getOrders(requireContext()));

        int ongoingCount = 0;
        for (Order o : allOrders) {
            if (o.isOngoing()) ongoingCount++;
        }
        tvOngoingCount.setText("进行中 " + ongoingCount);

        displayed.clear();
        for (Order o : allOrders) {
            if (matchesFilter(o)) displayed.add(o);
        }

        adapter.notifyDataSetChanged();
        emptyView.setVisibility(displayed.isEmpty() ? View.VISIBLE : View.GONE);
        orderList.setVisibility(displayed.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private boolean matchesFilter(Order o) {
        if (currentFilter == FILTER_ONGOING) return o.isOngoing();
        if (currentFilter == FILTER_HISTORY) return !o.isOngoing();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshOrders();
        handler.postDelayed(ticker, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(ticker);
        super.onDestroyView();
    }

    // ================== Adapter ==================
    class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(displayed.get(position));
        }

        @Override
        public int getItemCount() {
            return displayed.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvShop, tvTime, tvStatus, tvDishes, tvDetail, tvAddress;
            final TextView tvAmount, tvHint, tvStep1, tvStep2, tvStep3, tvStep4;
            final View llProgress;
            final TextView btnSecondary, btnPrimary;

            VH(View v) {
                super(v);
                tvShop = v.findViewById(R.id.tvOrderShop);
                tvTime = v.findViewById(R.id.tvOrderTime);
                tvStatus = v.findViewById(R.id.tvOrderStatus);
                tvDishes = v.findViewById(R.id.tvOrderDishes);
                tvDetail = v.findViewById(R.id.tvOrderDetail);
                tvAddress = v.findViewById(R.id.tvOrderAddress);
                tvAmount = v.findViewById(R.id.tvOrderAmount);
                tvHint = v.findViewById(R.id.tvStatusHint);
                llProgress = v.findViewById(R.id.llProgress);
                tvStep1 = v.findViewById(R.id.tvStep1);
                tvStep2 = v.findViewById(R.id.tvStep2);
                tvStep3 = v.findViewById(R.id.tvStep3);
                tvStep4 = v.findViewById(R.id.tvStep4);
                btnSecondary = v.findViewById(R.id.btnActionSecondary);
                btnPrimary = v.findViewById(R.id.btnActionPrimary);
            }

            void bind(final Order order) {
                tvShop.setText(order.restaurantName);
                tvStatus.setText(Order.statusText(order.status));
                tvDishes.setText(order.getDishSummary());
                tvDetail.setText(order.getItemDetail());
                tvAddress.setText("配送至：" + order.address);
                tvAmount.setText("¥" + order.total);
                tvHint.setText(Order.statusHint(order.status));
                tvTime.setText(formatTime(order.createTime));

                int statusColor = statusColor(order.status);
                tvStatus.setTextColor(statusColor);

                // 配送进度条（已完成的订单走满，已取消的不显示）
                if (order.status == Order.STATUS_CANCELLED) {
                    llProgress.setVisibility(View.GONE);
                } else {
                    llProgress.setVisibility(View.VISIBLE);
                    int stage = order.status + 1; // 0 待接单 -> 已完成 1 步
                    setStepColor(tvStep1, stage >= 1);
                    setStepColor(tvStep2, stage >= 2);
                    setStepColor(tvStep3, stage >= 3);
                    setStepColor(tvStep4, stage >= 4);
                }

                bindActions(order);
            }

            private void bindActions(final Order order) {
                final Context ctx = requireContext();

                switch (order.status) {
                    case Order.STATUS_PENDING:
                        btnSecondary.setText(R.string.action_cancel_order);
                        btnSecondary.setVisibility(View.VISIBLE);
                        btnSecondary.setOnClickListener(v -> confirmCancel(order));
                        btnPrimary.setText("催一下商家");
                        btnPrimary.setOnClickListener(v ->
                                Toast.makeText(ctx, "已提醒商家尽快接单～", Toast.LENGTH_SHORT).show());
                        break;

                    case Order.STATUS_ACCEPTED:
                        btnSecondary.setText(R.string.action_cancel_order);
                        btnSecondary.setVisibility(View.VISIBLE);
                        btnSecondary.setOnClickListener(v -> confirmCancel(order));
                        btnPrimary.setText(R.string.action_contact_rider);
                        btnPrimary.setOnClickListener(v ->
                                Toast.makeText(ctx, "正在呼叫商家…（演示）", Toast.LENGTH_SHORT).show());
                        break;

                    case Order.STATUS_DELIVERING:
                        btnSecondary.setText(R.string.action_contact_rider);
                        btnSecondary.setVisibility(View.VISIBLE);
                        btnSecondary.setOnClickListener(v ->
                                Toast.makeText(ctx, "骑手小李：139****2365（演示）", Toast.LENGTH_SHORT).show());
                        btnPrimary.setText("确认收货");
                        btnPrimary.setOnClickListener(v -> confirmReceived(order));
                        break;

                    case Order.STATUS_DELIVERED:
                        btnSecondary.setText(R.string.action_delete_order);
                        btnSecondary.setVisibility(View.VISIBLE);
                        btnSecondary.setOnClickListener(v -> confirmDelete(order));
                        if (order.rated) {
                            btnPrimary.setText(R.string.action_reorder);
                            btnPrimary.setOnClickListener(v -> reorder(order));
                        } else {
                            btnPrimary.setText(R.string.action_rate);
                            btnPrimary.setOnClickListener(v -> rateOrder(order));
                        }
                        break;

                    case Order.STATUS_CANCELLED:
                    default:
                        btnSecondary.setText(R.string.action_delete_order);
                        btnSecondary.setVisibility(View.VISIBLE);
                        btnSecondary.setOnClickListener(v -> confirmDelete(order));
                        btnPrimary.setText(R.string.action_reorder);
                        btnPrimary.setOnClickListener(v -> reorder(order));
                        break;
                }
            }

            private void confirmCancel(final Order order) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("取消订单")
                        .setMessage("确定要取消这笔订单吗？款项将原路退回。")
                        .setNegativeButton("再想想", null)
                        .setPositiveButton("确定取消", (dialog, which) -> {
                            OrderManager.cancelOrder(requireContext(), order.id);
                            Toast.makeText(requireContext(), "订单已取消", Toast.LENGTH_SHORT).show();
                            refreshOrders();
                        })
                        .show();
            }

            private void confirmReceived(Order order) {
                OrderManager.confirmReceived(requireContext(), order.id);
                Toast.makeText(requireContext(), "已确认收货，祝您用餐愉快！", Toast.LENGTH_SHORT).show();
                refreshOrders();
            }

            private void rateOrder(final Order order) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("评价订单")
                        .setMessage("感谢您的五星好评，2 元红包已放入账户～")
                        .setPositiveButton("好的", (dialog, which) -> {
                            OrderManager.markRated(requireContext(), order.id);
                            refreshOrders();
                        })
                        .show();
            }

            private void confirmDelete(final Order order) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("删除订单")
                        .setMessage("删除后本地记录将不再保留，确定删除吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (dialog, which) -> {
                            OrderManager.deleteOrder(requireContext(), order.id);
                            refreshOrders();
                        })
                        .show();
            }

            private void reorder(Order order) {
                if (getActivity() == null) return;
                Intent intent = new Intent(requireContext(), MenuActivity.class);
                intent.putExtra("restaurant_id", order.restaurantId);

                HashMap<String, Integer> prefill = new HashMap<>();
                for (Order.OrderItem item : order.items) {
                    prefill.put(item.name, item.count);
                }
                intent.putExtra("prefill_cart_map", prefill);
                startActivity(intent);
            }

            private int statusColor(int status) {
                int resId;
                switch (status) {
                    case Order.STATUS_PENDING:
                        resId = R.color.brand_orange;
                        break;
                    case Order.STATUS_ACCEPTED:
                        resId = R.color.blue;
                        break;
                    case Order.STATUS_DELIVERING:
                        resId = R.color.green;
                        break;
                    case Order.STATUS_DELIVERED:
                        resId = R.color.text_secondary;
                        break;
                    default:
                        resId = R.color.text_tertiary;
                        break;
                }
                return ContextCompat.getColor(requireContext(), resId);
            }

            private void setStepColor(TextView tv, boolean done) {
                tv.setTextColor(ContextCompat.getColor(requireContext(),
                        done ? R.color.brand_orange : R.color.grey_dot));
            }
        }
    }

    private static String formatTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(millis));
    }
}
