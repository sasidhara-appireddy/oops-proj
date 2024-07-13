package com.BitsBids.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BitsBids.model.Category;

public interface CategoryDao extends JpaRepository<Category, Integer> {

	List<Category> findByStatus(String status);

}
