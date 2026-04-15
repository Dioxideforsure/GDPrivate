package com.kuopan.Controller.ResultImpl;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // status code
    private String msg; // message
    private T data;

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> noPermission() {
        return new Result<>(401, "Permission Denied", null);
    }

    public static <T> Result<T> fail() {
        return new Result<>(500, "failed", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

}
