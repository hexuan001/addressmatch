package com.example.addressmatch.repository;

import com.example.addressmatch.entity.TableC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableCRepository extends JpaRepository<TableC, Long> {

    // 方法1：使用自定义JPQL查询（推荐）
    @Query("SELECT t FROM TableC t WHERE t.aId = :aId")
    List<TableC> findByAId(@Param("aId") Long aId);


    void deleteAll();
}