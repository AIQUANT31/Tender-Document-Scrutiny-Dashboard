import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-stats.component.html',
  styleUrl: './dashboard-stats.component.css'
})
export class DashboardStatsComponent {
  @Input() totalTenders = 0;
  @Input() openTenders = 0;
  @Input() closedTenders = 0;
  @Input() totalBids = 0;
  @Input() pendingBids = 0;
  @Input() approvedBids = 0;
  @Input() rejectedBids = 0;
  @Input() totalBidders = 0;
}
