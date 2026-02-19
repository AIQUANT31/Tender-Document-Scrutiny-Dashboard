package com.example.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Arrays;


@Configuration
@EnableCaching
public class CacheConfig {
    
    
    @Bean
    public CacheManager cacheManager() {
        // Define cache names (regions) for different types of data
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
    
        cacheManager.setCacheNames(Arrays.asList(
            // Bid caches
            "bidsByTender",      
            "bidsByBidder",      
            "bidStats",          
            "bidDetails",       
            "tenderBids",        
            "bidsWithTenders",   
            
            // Bidder caches
            "allBidders",        
            "bidderById",        
            "biddersByUser",     
            "bidderStats",       
            
            // Tender caches
            "allTenders",        
            "tenderById",        
            "tendersByUser",     
            
            // Dashboard caches
            "dashboardData"      
        ));
        
        return cacheManager;
    }
}
