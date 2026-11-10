package com.mentesvivas.backend.entity.link;

public enum LinkStatus {
    PENDING,
    ACTIVE,
    DECLINED,
    ENDED;

    public boolean isOpen() {
        return this == PENDING || this == ACTIVE;
    }
}
