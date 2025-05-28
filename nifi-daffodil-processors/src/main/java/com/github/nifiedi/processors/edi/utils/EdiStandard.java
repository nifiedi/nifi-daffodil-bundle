package com.github.nifiedi.processors.edi.utils;

/**
 * @author Chaojun.Xu
 * @date 2025/05/28 19:39
 */


public enum EdiStandard {
    X12("X12"),
    UNEDIFACT("UNEDIFACT");

    private String name;

    EdiStandard(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}