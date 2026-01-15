package com.example.addressmatch.repository;

import com.example.addressmatch.entity.TableB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableBRepository extends JpaRepository<TableB, Long> {
}