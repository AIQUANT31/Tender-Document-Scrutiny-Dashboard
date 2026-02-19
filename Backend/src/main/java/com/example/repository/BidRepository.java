package com.example.repository;

import com.example.entity.Bid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByTenderId(Long tenderId);
    List<Bid> findByBidderId(Long bidderId);
    Optional<Bid> findByTenderIdAndBidderId(Long tenderId, Long bidderId);
    boolean existsByTenderIdAndBidderId(Long tenderId, Long bidderId);
    List<Bid> findByTenderIdAndStatus(Long tenderId, String status);
    List<Bid> findByBidderIdAndStatus(Long bidderId, String status);
    
    long countByStatus(String status);
    
    @Query("SELECT b FROM Bid b ORDER BY b.createdAt DESC")
    List<Bid> findRecentBids(Pageable pageable);
    
    @Query("SELECT b.status, COUNT(b) FROM Bid b GROUP BY b.status")
    List<Object[]> countByStatusGrouped();
    
    @Query("SELECT COUNT(b) FROM Bid b WHERE b.isWinning = true")
    long countWinningBids();
    
    @Query("SELECT COUNT(b) FROM Bid b")
    long countTotalBids();
}
