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
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
    
        cacheManager.setCacheNames(Arrays.asList(
            
            "bidsByTender",      
            "bidsByBidder",      
            "bidStats",          
            "bidDetails",       
            "tenderBids",        
            "bidsWithTenders",   
          
            "allBidders",        
            "bidderById",        
            "biddersByUser",     
            "bidderStats",       
            
            
            "allTenders",        
            "tenderById",        
            "tendersByUser",     
            
           
            "dashboardData"      
        ));
        
        return cacheManager;
    }
}
