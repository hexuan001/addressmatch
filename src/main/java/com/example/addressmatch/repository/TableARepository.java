package com.example.addressmatch.repository;

import com.example.addressmatch.entity.TableA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableARepository extends JpaRepository<TableA, Long> {
}