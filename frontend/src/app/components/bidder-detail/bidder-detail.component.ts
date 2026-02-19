import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BidderData } from '../../models/bidder.model';

@Component({
  selector: 'app-bidder-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bidder-detail.component.html',
  styleUrl: './bidder-detail.component.css'
})
export class BidderDetailComponent implements OnChanges {
  @Input() showBidderDetail: boolean = false;
  @Input() bidderId: number | null = null;
  
  @Output() close = new EventEmitter<void>();

  // Used in template
  selectedBidder: BidderData | null = null;
  loading = false;

  constructor(private http: HttpClient) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['bidderId'] && this.bidderId && this.showBidderDetail) {
      this.loadBidderDetails();
    }
  }

  loadBidderDetails() {
    if (!this.bidderId) return;

    this.loading = true;
    this.http.get<BidderData>(`http://localhost:8080/api/bidders/${this.bidderId}`).subscribe({
      next: (data) => {
        this.selectedBidder = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error fetching bidder details:', error);
        alert('Error loading bidder details');
        this.loading = false;
        this.onClose();
      }
    });
  }

  onClose() {
    this.close.emit();
    this.selectedBidder = null;
  }

  // Format date helper
  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  }

  // Get status badge class
  getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE': return 'badge-success';
      case 'INACTIVE': return 'badge-danger';
      default: return 'badge-warning';
    }
  }
}
