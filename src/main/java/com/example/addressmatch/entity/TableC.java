package com.example.addressmatch.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "table_c")
public class TableC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "a_id")
    private Long aId;

    @Column(name = "address_b", columnDefinition = "TEXT")
    private String addressB;

    @Column(name = "match_score")
    private Double matchScore;

    // 添加无参构造函数（重要）
    public TableC() {}

    // 添加全参构造函数（可选）
    public TableC(Long aId, String addressB, Double matchScore) {
        this.aId = aId;
        this.addressB = addressB;
        this.matchScore = matchScore;
    }
}