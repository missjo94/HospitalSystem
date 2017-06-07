package com.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

}
