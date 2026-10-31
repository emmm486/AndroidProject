package com.example.application1;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 应用主容器：底部三个导航栏 —— 点餐 / 订单 / 我的。
 * 三个页面的实际内容分别在 HomeFragment / OrdersFragment / ProfileFragment 中。
 */
public class MainActivity extends AppCompatActivity {

    /** 从外部（例如支付成功）跳进来时，用这个 key 指定要显示的 Tab */
    public static final String EXTRA_TAB = "extra_tab";
    public static final String TAB_HOME = "home";
    public static final String TAB_ORDERS = "orders";
    public static final String TAB_PROFILE = "profile";

    private static final String KEY_CURRENT_TAB = "key_current_tab";

    private BottomNavigationView bottomNav;

    private HomeFragment homeFragment;
    private OrdersFragment ordersFragment;
    private ProfileFragment profileFragment;

    /** 当前显示的 Fragment */
    private String currentTab = TAB_HOME;

    /** 程序内主动调用 setSelectedItemId 时置为 true，避免监听器回调再次进入 showTab 造成重入 */
    private boolean syncingNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onNavSelected);

        // 屏幕旋转等配置变更后，Fragment 会被系统重建，这里重新取回引用，避免重复创建实例
        if (savedInstanceState != null) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment savedHome = fm.findFragmentByTag(TAB_HOME);
            Fragment savedOrders = fm.findFragmentByTag(TAB_ORDERS);
            Fragment savedProfile = fm.findFragmentByTag(TAB_PROFILE);
            if (savedHome instanceof HomeFragment) homeFragment = (HomeFragment) savedHome;
            if (savedOrders instanceof OrdersFragment) ordersFragment = (OrdersFragment) savedOrders;
            if (savedProfile instanceof ProfileFragment) profileFragment = (ProfileFragment) savedProfile;

            currentTab = savedInstanceState.getString(KEY_CURRENT_TAB, TAB_HOME);
        }

        // 外部跳转指定 Tab（优先级高于恢复的状态）
        String target = getIntent() != null ? getIntent().getStringExtra(EXTRA_TAB) : null;
        if (target != null) {
            currentTab = target;
        }
        showTab(currentTab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String target = intent != null ? intent.getStringExtra(EXTRA_TAB) : null;
        if (target != null) {
            showTab(target);
        }
    }

    private boolean onNavSelected(@NonNull android.view.MenuItem item) {
        // 同步底部导航选中态时触发的回调直接放行，避免重复执行切换逻辑
        if (syncingNav) return true;

        int id = item.getItemId();
        if (id == R.id.nav_home) {
            showTab(TAB_HOME);
        } else if (id == R.id.nav_orders) {
            showTab(TAB_ORDERS);
        } else if (id == R.id.nav_profile) {
            showTab(TAB_PROFILE);
        } else {
            return false;
        }
        return true;
    }

    /**
     * 切换 Tab：三个 Fragment 常驻内存，只做 show/hide，保留各自的滚动位置和状态。
     * 注意：
     * 1. 不能对「还没 add 过」的 Fragment 调 hide()，否则它会被加进去但处于隐藏状态（白屏）。
     * 2. 用 commitNow 同步提交，保证 isAdded() 立即生效，防止快速重复调用时重复 add 导致崩溃。
     */
    public void showTab(String tab) {
        currentTab = tab;

        Fragment target = obtainFragment(tab);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (!target.isAdded()) {
            ft.add(R.id.fragmentContainer, target, tab);
        } else if (target.isHidden()) {
            ft.show(target);
        }

        // 只隐藏「已经添加过」的其他 Fragment
        Fragment[] all = {homeFragment, ordersFragment, profileFragment};
        for (Fragment f : all) {
            if (f == null || f == target) continue;
            if (f.isAdded() && !f.isHidden()) ft.hide(f);
        }
        ft.commitNowAllowingStateLoss();

        // 同步底部导航的选中态
        int menuId = tabId(tab);
        if (bottomNav.getSelectedItemId() != menuId) {
            syncingNav = true;
            bottomNav.setSelectedItemId(menuId);
            syncingNav = false;
        }

        // 每次进入这两个页面都刷新一次数据（订单状态、个人资料可能已变化）
        if (TAB_ORDERS.equals(tab) && ordersFragment != null) {
            ordersFragment.refreshOrders();
        } else if (TAB_PROFILE.equals(tab) && profileFragment != null) {
            profileFragment.refreshData();
        }
    }

    /** 供 Fragment 之间联动使用，例如「我的」页快捷入口跳到订单页并设置筛选 */
    @Nullable
    public OrdersFragment getOrdersFragment() {
        return ordersFragment;
    }

    @Nullable
    public ProfileFragment getProfileFragment() {
        return profileFragment;
    }

    private Fragment obtainFragment(String tab) {
        if (TAB_ORDERS.equals(tab)) {
            if (ordersFragment == null) ordersFragment = new OrdersFragment();
            return ordersFragment;
        }
        if (TAB_PROFILE.equals(tab)) {
            if (profileFragment == null) profileFragment = new ProfileFragment();
            return profileFragment;
        }
        if (homeFragment == null) homeFragment = new HomeFragment();
        return homeFragment;
    }

    private int tabId(String tab) {
        if (TAB_ORDERS.equals(tab)) return R.id.nav_orders;
        if (TAB_PROFILE.equals(tab)) return R.id.nav_profile;
        return R.id.nav_home;
    }

    public String getCurrentTab() {
        return currentTab;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_TAB, currentTab);
    }

    @Override
    public void onBackPressed() {
        // 处在非「点餐」页时，先回到点餐页；已在点餐页才退出
        if (!TAB_HOME.equals(currentTab)) {
            showTab(TAB_HOME);
        } else {
            super.onBackPressed();
        }
    }
}
