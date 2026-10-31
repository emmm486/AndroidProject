package com.example.application1;

/**
 * 一条收货地址。
 */
public class Address {

    public String id;
    public String name;     // 联系人
    public String phone;    // 手机号
    public String detail;   // 详细地址
    public boolean isDefault;

    public Address(String id, String name, String phone, String detail, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.detail = detail;
        this.isDefault = isDefault;
    }
}
