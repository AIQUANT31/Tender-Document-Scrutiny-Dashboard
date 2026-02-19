import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BidderData } from '../../models/bidder.model';

interface BidderStats {
  totalBidders: number;
  activeBidders: number;
  inactiveBidders: number;
  totalBidsPlaced: number;
  winningBids: number;
}

@Component({
  selector: 'app-bidder-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bidder-stats.component.html',
  styleUrl: './bidder-stats.component.css'
})
export class BidderStatsComponent implements OnChanges {
  @Input() bidders: BidderData[] = [];

  stats: BidderStats = {
    totalBidders: 0,
    activeBidders: 0,
    inactiveBidders: 0,
    totalBidsPlaced: 0,
    winningBids: 0
  };

  ngOnChanges(changes: SimpleChanges) {
    if (changes['bidders']) {
      this.calculateStats();
    }
  }

  private calculateStats() {
    if (!this.bidders || this.bidders.length === 0) {
      this.stats = {
        totalBidders: 0,
        activeBidders: 0,
        inactiveBidders: 0,
        totalBidsPlaced: 0,
        winningBids: 0
      };
      return;
    }

    this.stats = {
      totalBidders: this.bidders.length,
      activeBidders: this.bidders.filter(b => b.status?.toUpperCase() === 'ACTIVE').length,
      inactiveBidders: this.bidders.filter(b => b.status?.toUpperCase() === 'INACTIVE').length,
      totalBidsPlaced: this.bidders.reduce((sum, b) => sum + (b.totalBids || 0), 0),
      winningBids: this.bidders.reduce((sum, b) => sum + (b.winningBids || 0), 0)
    };
  }

  // Get win rate percentage
  get winRate(): number {
    if (this.stats.totalBidsPlaced === 0) return 0;
    return Math.round((this.stats.winningBids / this.stats.totalBidsPlaced) * 100);
  }
}
