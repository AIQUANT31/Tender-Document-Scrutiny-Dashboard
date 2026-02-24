import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

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

@Component({
  selector: 'app-dashboard-activity',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-activity.component.html',
  styleUrl: './dashboard-activity.component.css'
})
export class DashboardActivityComponent {
  @Input() recentTenders: TenderSummary[] = [];
  @Input() recentBids: BidSummary[] = [];
  @Input() recentBidders: BidderSummary[] = [];

  getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'OPEN':
      case 'ACTIVE':
      case 'APPROVED':
      case 'WINNING':
        return 'status-success';
      case 'CLOSED':
      case 'REJECTED':
        return 'status-danger';
      case 'PENDING':
        return 'status-warning';
      default:
        return 'status-default';
    }
  }
}
