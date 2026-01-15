package com.example.addressmatch.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "table_a")
public class TableA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_a", columnDefinition = "TEXT")
    private String addressA;
}