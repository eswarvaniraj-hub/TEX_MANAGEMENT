package com.textile.accounts.masterdata.model;

import jakarta.persistence.*;

@Entity
@Table(name = "gst_state")
public class GstState {

    @Id
    @Column(length = 2, nullable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    public GstState() {}

    public GstState(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
