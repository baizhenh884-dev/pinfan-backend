package com.pinfan.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {
    private Integer code; //编码： 200 成功
                          //      400 客户端错误（参数校验失败、权限不足等）
                          //      500 服务器错误（系统异常）
    private String msg; //错误信息
    private T data; //数据

    public static <T> R<T> ok() {
        return new R<>(200, "成功", null);
    }

    public static <T> R<T> ok(T data){
        return new R<>(200, "成功", data);
    }

    public static <T> R<T> ok(String msg, T data){
        return new R<>(200, msg, data);
    }

    public static <T> R<T> fail(String msg){
        return new R<>(500, msg, null);
    }

    public static <T> R<T> fail(Integer code, String msg){
        return new R<>(code, msg, null);
    }


}
