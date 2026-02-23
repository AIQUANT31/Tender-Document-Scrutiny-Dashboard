import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { NavComponent } from '../../components/nav/nav.component';
import { DashboardStatsComponent } from '../../components/dashboard-stats/dashboard-stats.component';
import { DashboardActivityComponent } from '../../components/dashboard-activity/dashboard-activity.component';
import { DashboardActionsComponent } from '../../components/dashboard-actions/dashboard-actions.component';

interface TenderSummary {
  id: number;
  name: string;
  status: string;
  budget: string;
  createdAt: string;
}

interface BidSummary {
  id: number;
  tenderId: number;
  tenderName: string;
  bidAmount: string;
  status: string;
  createdAt: string;
}

interface BidderSummary {
  id: number;
  companyName: string;
  email: string;
  status: string;
  totalBids: number;
  createdAt: string;
}

interface DashboardData {
  message: string;
  username: string;
  totalTenders: number;
  openTenders: number;
  closedTenders: number;
  totalBids: number;
  pendingBids: number;
  approvedBids: number;
  rejectedBids: number;
  totalBidders: number;
  activeBidders: number;
  inactiveBidders: number;
  recentTenders: TenderSummary[];
  recentBids: BidSummary[];
  recentBidders: BidderSummary[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    NavComponent,
    DashboardStatsComponent,
    DashboardActivityComponent,
    DashboardActionsComponent
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  username = 'User';
  loading = true;
  dashboardData: DashboardData | null = null;
  
  // Stats
  totalTenders = 0;
  openTenders = 0;
  closedTenders = 0;
  totalBids = 0;
  pendingBids = 0;
  approvedBids = 0;
  rejectedBids = 0;
  totalBidders = 0;
  activeBidders = 0;
  inactiveBidders = 0;
  
  // Recent data
  recentTenders: TenderSummary[] = [];
  recentBids: BidSummary[] = [];
  recentBidders: BidderSummary[] = [];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      this.username = localStorage.getItem('username') || 'User';
    }
  }

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.loading = true;
    
    const apiUrl = `http://localhost:8080/api/dashboard/data?username=${encodeURIComponent(this.username)}`;
    
    this.http.get<DashboardData>(apiUrl).subscribe({
      next: (data) => {
        console.log('Dashboard data loaded:', data);
        this.dashboardData = data;
        
        // Set stats
        this.totalTenders = data.totalTenders || 0;
        this.openTenders = data.openTenders || 0;
        this.closedTenders = data.closedTenders || 0;
        this.totalBids = data.totalBids || 0;
        this.pendingBids = data.pendingBids || 0;
        this.approvedBids = data.approvedBids || 0;
        this.rejectedBids = data.rejectedBids || 0;
        this.totalBidders = data.totalBidders || 0;
        this.activeBidders = data.activeBidders || 0;
        this.inactiveBidders = data.inactiveBidders || 0;
        
        // Set recent data
        this.recentTenders = data.recentTenders || [];
        this.recentBids = data.recentBids || [];
        this.recentBidders = data.recentBidders || [];
        
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading dashboard data:', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
