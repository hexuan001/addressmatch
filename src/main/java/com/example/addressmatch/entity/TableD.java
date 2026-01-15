package com.example.addressmatch.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "table_d")
public class TableD {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "b_id")
    private Long bId;

    @Column(name = "address_b", columnDefinition = "TEXT", nullable = false)
    private String addressB;          // B表地址

    @Column(name = "status")
    private String status = "PENDING"; // 状态：PENDING/ADDED/REJECTED
}