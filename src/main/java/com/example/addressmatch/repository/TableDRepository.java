package com.example.addressmatch.repository;

import com.example.addressmatch.entity.TableD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableDRepository extends JpaRepository<TableD, Long> {
    List<TableD> findByStatus(String status);
    long countByStatus(String status);
}