package com.example.merkletree.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private int code;
    private String msg;
    private T data;

    // 添加静态工厂方法
    public static <T> R<T> success(T data) {
        return new R<>(200, "成功", data);
    }

    public static <T> R<T> error(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> success(String message, T data) {
        return new R<>(200, message, data);
    }
}