package com.example.addressmatch.entity;

import javax.persistence.*;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "table_d")
public class TableD {

    public enum Status {
        PENDING,     // 待处理
        CONFIRMED,   // 已确认有效
        REJECTED     // 已驳回
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_b")
    private String addressB;  // 原始地址

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;    // 状态
}