package com.soulbuddy.be.domain.counseling.repository;

import com.soulbuddy.be.domain.counseling.entity.Counselingcenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Counselingcenterrepository extends JpaRepository<Counselingcenter, Long> {

    @Query("SELECT c FROM Counselingcenter c " +
            "WHERE c.isActive = true " +
            "ORDER BY c.isEmergency DESC, c.sortOrder ASC")
    List<Counselingcenter> findAllActiveOrderByEmergencyAndSort();

    @Query("SELECT c FROM Counselingcenter c " +
            "WHERE c.isActive = true AND (c.region = :region OR c.region IS NULL) " +
            "ORDER BY c.isEmergency DESC, c.sortOrder ASC")
    List<Counselingcenter> findByRegionAndActive(@Param("region") String region);

    @Query("SELECT c FROM Counselingcenter c " +
            "WHERE c.isActive = true AND c.isEmergency = true " +
            "ORDER BY c.sortOrder ASC")
    List<Counselingcenter> findAllEmergency();

    Optional<Counselingcenter> findByPhone(String phone);

    @Query("SELECT c FROM Counselingcenter c " +
            "WHERE c.isActive = true AND c.name LIKE %:name% " +
            "ORDER BY c.isEmergency DESC, c.sortOrder ASC")
    List<Counselingcenter> findByNameContainingIgnoreCase(@Param("name") String name);

    List<Counselingcenter> findByIsActiveTrueOrderByIsEmergencyDescSortOrderAsc();
}