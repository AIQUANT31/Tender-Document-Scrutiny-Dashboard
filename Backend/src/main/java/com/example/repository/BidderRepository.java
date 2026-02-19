package com.example.repository;

import com.example.entity.Bidder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidderRepository extends JpaRepository<Bidder, Long> {
    List<Bidder> findByCreatedBy(Long createdBy);
    List<Bidder> findByStatus(String status);
    List<Bidder> findByType(String type);
    Optional<Bidder> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Bidder> findByCompanyNameContainingIgnoreCase(String companyName);
    
    long countByStatus(String status);
    
    @Query("SELECT b FROM Bidder b ORDER BY b.createdAt DESC")
    List<Bidder> findRecentBidders(Pageable pageable);
    
    @Query("SELECT COUNT(b) FROM Bidder b")
    long countTotalBidders();
}
