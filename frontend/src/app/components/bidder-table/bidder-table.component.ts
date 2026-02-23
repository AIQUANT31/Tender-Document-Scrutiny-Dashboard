import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BidderData } from '../../models/bidder.model';

@Component({
  selector: 'app-bidder-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bidder-table.component.html',
  styleUrl: './bidder-table.component.css'
})
export class BidderTableComponent {
  @Input() bidders: BidderData[] = [];
  @Input() userId: number | null = null;
  
  @Output() viewBidderDetails = new EventEmitter<number>();
  @Output() editBidder = new EventEmitter<BidderData>();
  @Output() deleteSuccess = new EventEmitter<void>();
  @Output() error = new EventEmitter<string>();

  constructor(private http: HttpClient) {}

  onView(id: number) {
    this.viewBidderDetails.emit(id);
  }

  onEdit(bidder: BidderData) {
    this.editBidder.emit(bidder);
  }

  onDelete(id: number, event: Event) {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this bidder?')) {
      this.deleteBidder(id);
    }
  }

  private deleteBidder(bidderId: number) {
    if (!this.userId) {
      this.error.emit('Please login first to delete a bidder.');
      return;
    }

    this.http.delete<any>(`http://localhost:8080/api/bidders/${bidderId}?userId=${this.userId}`).subscribe({
      next: (response) => {
        if (response.success) {
          this.deleteSuccess.emit();
        } else {
          this.error.emit(response.message);
        }
      },
      error: (error) => {
        console.error('Error deleting bidder:', error);
        this.error.emit('Error deleting bidder');
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'ACTIVE': return 'status-active';
      case 'INACTIVE': return 'status-inactive';
      case 'PENDING': return 'status-pending';
      default: return '';
    }
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  }
}
