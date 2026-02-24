package com.example.repository;

import com.example.entity.Tender;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TenderRepository extends JpaRepository<Tender, Long> {
    
    List<Tender> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    List<Tender> findByStatusOrderByCreatedAtDesc(String status);
    
    long countByStatus(String status);
    
    @Query("SELECT t FROM Tender t ORDER BY t.createdAt DESC")
    List<Tender> findRecentTenders(Pageable pageable);
    
    @Query("SELECT t FROM Tender t ORDER BY t.createdAt DESC")
    List<Tender> findAllTendersSorted();
    
    @Query("SELECT t.status, COUNT(t) FROM Tender t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();
    
    
    @Query("SELECT t FROM Tender t WHERE t.status = 'OPEN' AND t.deadline <= :currentTime")
    List<Tender> findOpenTendersWithExpiredDeadline(@Param("currentTime") LocalDateTime currentTime);
}
