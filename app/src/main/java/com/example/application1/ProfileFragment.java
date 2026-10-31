package com.example.application1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

/**
 * 「我的」页：个人资料、订单快捷入口、常用功能。
 */
public class ProfileFragment extends Fragment {

    private TextView tvNickname, tvPhone, tvMemberLevel;
    private TextView tvFavoriteCount, tvCouponCount, tvAddressCount;
    private TextView tvLabelPending, tvLabelDelivering, tvLabelUnrated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNickname = view.findViewById(R.id.tvNickname);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvMemberLevel = view.findViewById(R.id.tvMemberLevel);
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);
        tvCouponCount = view.findViewById(R.id.tvCouponCount);
        tvAddressCount = view.findViewById(R.id.tvAddressCount);
        tvLabelPending = view.findViewById(R.id.tvLabelPending);
        tvLabelDelivering = view.findViewById(R.id.tvLabelDelivering);
        tvLabelUnrated = view.findViewById(R.id.tvLabelUnrated);

        view.findViewById(R.id.tvEditProfile).setOnClickListener(v -> showEditDialog());

        setupOrderShortcuts(view);
        setupFunctionRows(view);

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到本页刷新数据（昵称/手机号/地址/订单数可能变了）
        refreshData();
    }

    /** 供外部（MainActivity 切换 Tab 时）调用 */
    public void refreshData() {
        if (getView() == null || getContext() == null) return;

        tvNickname.setText(UserManager.getNickname(requireContext()));
        tvPhone.setText(UserManager.getMaskedPhone(requireContext()));
        tvMemberLevel.setText(UserManager.getMemberLevel(requireContext()));

        tvFavoriteCount.setText(UserManager.getFavorites(requireContext()) + " 家 ›");
        tvCouponCount.setText(UserManager.getCoupons(requireContext()) + " 张可用 ›");
        tvAddressCount.setText(AddressManager.count(requireContext()) + " 个 ›");

        int pending = 0, delivering = 0, unrated = 0;
        for (Order o : OrderManager.getOrders(requireContext())) {
            if (o.status == Order.STATUS_PENDING || o.status == Order.STATUS_ACCEPTED) {
                pending++;
            } else if (o.status == Order.STATUS_DELIVERING) {
                delivering++;
            } else if (o.status == Order.STATUS_DELIVERED && !o.rated) {
                unrated++;
            }
        }
        tvLabelPending.setText(pending > 0 ? "待接单 (" + pending + ")" : "待接单");
        tvLabelDelivering.setText(delivering > 0 ? "配送中 (" + delivering + ")" : "配送中");
        tvLabelUnrated.setText(unrated > 0 ? "待评价 (" + unrated + ")" : "待评价");
    }

    // ================== 订单快捷入口 ==================
    private void setupOrderShortcuts(View view) {
        view.findViewById(R.id.tvAllOrders).setOnClickListener(v -> gotoOrders(OrdersFragment.FILTER_ALL));
        view.findViewById(R.id.shortcutPending).setOnClickListener(v -> gotoOrders(OrdersFragment.FILTER_ONGOING));
        view.findViewById(R.id.shortcutDelivering).setOnClickListener(v -> gotoOrders(OrdersFragment.FILTER_ONGOING));
        view.findViewById(R.id.shortcutUnrated).setOnClickListener(v -> gotoOrders(OrdersFragment.FILTER_HISTORY));
        view.findViewById(R.id.shortcutRefund).setOnClickListener(v -> gotoOrders(OrdersFragment.FILTER_ALL));
    }

    private void gotoOrders(int filter) {
        if (!(getActivity() instanceof MainActivity)) return;
        MainActivity main = (MainActivity) getActivity();
        main.showTab(MainActivity.TAB_ORDERS);
        OrdersFragment fragment = main.getOrdersFragment();
        if (fragment != null) {
            fragment.switchToFilter(filter);
        }
    }

    // ================== 常用功能 ==================
    private void setupFunctionRows(View view) {
        view.findViewById(R.id.rowAddress).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddressActivity.class)));

        view.findViewById(R.id.rowFavorite).setOnClickListener(v ->
                toast("共收藏 " + UserManager.getFavorites(requireContext()) + " 家店铺"));

        view.findViewById(R.id.rowCoupon).setOnClickListener(v ->
                toast("可用红包 " + UserManager.getCoupons(requireContext()) + " 张"));

        view.findViewById(R.id.rowMember).setOnClickListener(v ->
                toast("当前等级：" + UserManager.getMemberLevel(requireContext())
                        + "，继续下单可升级领取更多优惠"));

        view.findViewById(R.id.rowService).setOnClickListener(v ->
                toast("客服热线：400-000-1234（9:00-22:00）"));

        view.findViewById(R.id.rowFeedback).setOnClickListener(v ->
                toast("感谢反馈！我们会在 24 小时内回复"));

        view.findViewById(R.id.rowAbout).setOnClickListener(v ->
                toast("校园外卖 v1.0 —— Android 课程演示项目"));
    }

    private void toast(String msg) {
        if (getContext() == null) return;
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    // ================== 编辑资料（昵称 + 手机号） ==================
    private void showEditDialog() {
        if (getContext() == null) return;

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        final EditText inputNickname = new EditText(requireContext());
        inputNickname.setHint("请输入昵称");
        inputNickname.setSingleLine(true);
        inputNickname.setText(UserManager.getNickname(requireContext()));
        inputNickname.setSelection(inputNickname.getText().length());
        container.addView(inputNickname);

        final EditText inputPhone = new EditText(requireContext());
        inputPhone.setHint("请输入 11 位手机号");
        inputPhone.setSingleLine(true);
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        String phone = UserManager.getPhone(requireContext());
        inputPhone.setText(phone);
        container.addView(inputPhone);

        new AlertDialog.Builder(requireContext())
                .setTitle("编辑资料")
                .setView(container)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String nickname = inputNickname.getText().toString().trim();
                    String newPhone = inputPhone.getText().toString().trim();
                    if (nickname.isEmpty()) {
                        Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPhone.matches("\\d{11}")) {
                        Toast.makeText(requireContext(), "请输入 11 位手机号", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserManager.setNickname(requireContext(), nickname);
                    UserManager.setPhone(requireContext(), newPhone);
                    Toast.makeText(requireContext(), "资料已更新", Toast.LENGTH_SHORT).show();
                    refreshData();
                })
                .show();
    }

    private void showLogoutDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("退出登录")
                .setMessage("确定要退出当前账号吗？")
                .setNegativeButton("再想想", null)
                .setPositiveButton("退出", (dialog, which) ->
                        Toast.makeText(requireContext(), "已退出登录（演示）", Toast.LENGTH_SHORT).show())
                .show();
    }
}
