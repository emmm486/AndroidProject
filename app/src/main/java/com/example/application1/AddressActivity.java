package com.example.application1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 收货地址管理页：查看 / 新增 / 编辑 / 删除 / 设为默认。
 */
public class AddressActivity extends AppCompatActivity {

    private RecyclerView addressList;
    private AddressAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddAddress).setOnClickListener(v -> showAddressDialog(null));

        addressList = findViewById(R.id.addressList);
        addressList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressAdapter();
        addressList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        adapter.submit(AddressManager.getAddresses(this));
    }

    // ================== 新增 / 编辑弹窗 ==================
    private void showAddressDialog(@Nullable final Address editing) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        final EditText inputName = new EditText(this);
        inputName.setHint("联系人姓名");
        inputName.setSingleLine(true);
        container.addView(inputName);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("手机号码");
        inputPhone.setSingleLine(true);
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        container.addView(inputPhone);

        final EditText inputDetail = new EditText(this);
        inputDetail.setHint("详细地址（小区 / 楼栋 / 门牌号）");
        inputDetail.setSingleLine(true);
        container.addView(inputDetail);

        final CheckBox checkDefault = new CheckBox(this);
        checkDefault.setText("设为默认地址");
        container.addView(checkDefault);

        if (editing != null) {
            inputName.setText(editing.name);
            inputName.setSelection(inputName.getText().length());
            inputPhone.setText(editing.phone);
            inputDetail.setText(editing.detail);
            checkDefault.setChecked(editing.isDefault);
        }

        new AlertDialog.Builder(this)
                .setTitle(editing == null ? "新增收货地址" : "编辑收货地址")
                .setView(container)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String phone = inputPhone.getText().toString().trim();
                    String detail = inputDetail.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "请填写联系人姓名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!phone.matches("\\d{11}")) {
                        Toast.makeText(this, "请输入 11 位手机号", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (detail.isEmpty()) {
                        Toast.makeText(this, "请填写详细地址", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (editing == null) {
                        AddressManager.add(this, name, phone, detail, checkDefault.isChecked());
                        Toast.makeText(this, "地址已添加", Toast.LENGTH_SHORT).show();
                    } else {
                        editing.name = name;
                        editing.phone = phone;
                        editing.detail = detail;
                        if (checkDefault.isChecked()) {
                            editing.isDefault = true;
                        }
                        AddressManager.update(this, editing);
                        Toast.makeText(this, "地址已更新", Toast.LENGTH_SHORT).show();
                    }
                    refreshList();
                })
                .show();
    }

    // ================== 列表适配器 ==================
    private class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.VH> {

        private final List<Address> data = new ArrayList<>();

        void submit(List<Address> list) {
            data.clear();
            data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View item = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_address, parent, false);
            return new VH(item);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Address address = data.get(position);
            holder.tvName.setText(address.name);
            holder.tvPhone.setText(address.phone);
            holder.tvDetail.setText(address.detail);
            holder.tvDefaultTag.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);
            holder.btnSetDefault.setVisibility(address.isDefault ? View.GONE : View.VISIBLE);

            holder.btnSetDefault.setOnClickListener(v -> {
                AddressManager.setDefault(AddressActivity.this, address.id);
                Toast.makeText(AddressActivity.this, "已设为默认地址", Toast.LENGTH_SHORT).show();
                refreshList();
            });

            holder.btnEdit.setOnClickListener(v -> showAddressDialog(address));

            holder.btnDelete.setOnClickListener(v -> new AlertDialog.Builder(AddressActivity.this)
                    .setTitle("删除地址")
                    .setMessage("确定删除这条收货地址吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (dialog, which) -> {
                        AddressManager.delete(AddressActivity.this, address.id);
                        Toast.makeText(AddressActivity.this, "地址已删除", Toast.LENGTH_SHORT).show();
                        refreshList();
                    })
                    .show());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvDetail, tvDefaultTag, btnSetDefault, btnEdit, btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvAddressName);
                tvPhone = itemView.findViewById(R.id.tvAddressPhone);
                tvDetail = itemView.findViewById(R.id.tvAddressDetail);
                tvDefaultTag = itemView.findViewById(R.id.tvDefaultTag);
                btnSetDefault = itemView.findViewById(R.id.btnSetDefault);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
