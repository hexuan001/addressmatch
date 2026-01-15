package com.example.addressmatch.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "table_b")
public class TableB {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_b", columnDefinition = "TEXT")
    private String addressB;
}