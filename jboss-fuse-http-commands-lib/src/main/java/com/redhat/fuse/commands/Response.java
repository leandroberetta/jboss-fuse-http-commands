package com.redhat.fuse.commands;

import java.io.Serializable;

public class Response implements Serializable {

    public static Integer SUCCESS = 0;
    public static Integer ERROR = -1;

    private Integer status;
    private Object data;

    public Object getData() {
        return data;
    }
    public Integer getStatus() {
        return status;
    }
    public void setStatus(Integer status) {
        this.status = status;
    }
    public void setData(Object data) {
        this.data = data;
    }
}
