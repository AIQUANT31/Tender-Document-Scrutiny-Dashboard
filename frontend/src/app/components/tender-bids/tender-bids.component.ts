import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface BidWithBidder {
  bidId: number;
  tenderId: number;
  bidderId: number;
  bidderCompanyName: string;
  bidderEmail: string;
  bidderPhone: string;
  bidderType: string;
  bidderContactPerson: string;
  bidAmount: number;
  proposalText: string;
  status: string;
  isWinning: boolean;
  contactNumber: string;
  createdAt: string | null;
  updatedAt: string | null;
  documentPath: string | null;
  documentPaths: string | null;
}

interface TenderInfo {
  id: number;
  name: string;
  budget: number;
  status: string;
}

@Component({
  selector: 'app-tender-bids',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tender-bids.component.html',
  styleUrl: './tender-bids.component.css'
})
export class TenderBidsComponent implements OnInit {
  @Input() tender: TenderInfo | null = null;
  @Output() onClose = new EventEmitter<void>();
  @Output() onUpdateBidStatus = new EventEmitter<{ bidId: number; status: string }>();

  bids: BidWithBidder[] = [];
  loading = false;
  error = '';
  selectedBid: BidWithBidder | null = null;
  showBidDetail = false;

  // Statistics
  stats = {
    totalBids: 0,
    pendingBids: 0,
    acceptedBids: 0,
    rejectedBids: 0
  };

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    if (this.tender) {
      this.loadBids();
    }
  }

  loadBids() {
    if (!this.tender) return;

    this.loading = true;
    this.error = '';

    this.http.get<BidWithBidder[]>(`http://localhost:8080/api/bids/tender/${this.tender.id}/with-bidders`).subscribe({
      next: (bids) => {
        this.bids = bids;
        this.calculateStats();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading bids:', err);
        this.error = 'Error loading bids. Please try again.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  calculateStats() {
    this.stats.totalBids = this.bids.length;
    this.stats.pendingBids = this.bids.filter(b => b.status === 'PENDING').length;
    this.stats.acceptedBids = this.bids.filter(b => b.status === 'ACCEPTED' || b.status === 'WINNING').length;
    this.stats.rejectedBids = this.bids.filter(b => b.status === 'REJECTED').length;
  }

  close() {
    this.onClose.emit();
  }

  viewBidDetails(bid: BidWithBidder) {
       console.log('Viewing bid details:', bid);
    this.selectedBid = bid;
    this.showBidDetail = true;
        console.log('showBidDetail set to true');
    setTimeout(() => {
      this.cdr.detectChanges();
    }, 100);
  }

  closeBidDetail() {
    this.showBidDetail = false;
    this.selectedBid = null;
  }

  updateBidStatus(bidId: number, status: string) {
    this.http.put<any>(`http://localhost:8080/api/bids/${bidId}/status?status=${status}`, {}).subscribe({
      next: (response) => {
        if (response.success) {
          alert(`Bid ${status.toLowerCase()} successfully!`);
          this.loadBids();
          this.closeBidDetail();
          this.onUpdateBidStatus.emit({ bidId, status });
        } else {
          alert(response.message || 'Error updating bid status');
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error updating bid status:', err);
        alert('Error updating bid status. Please try again.');
        this.cdr.detectChanges();
      }
    });
  }

  acceptBid(bidId: number) {
    if (confirm('Are you sure you want to accept this bid? This will mark it as the winning bid.')) {
      this.updateBidStatus(bidId, 'WINNING');
    }
  }

  rejectBid(bidId: number) {
    if (confirm('Are you sure you want to reject this bid?')) {
      this.updateBidStatus(bidId, 'REJECTED');
    }
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'N/A';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR'
    }).format(amount);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'ACCEPTED':
      case 'WINNING':
        return 'status-accepted';
      case 'REJECTED':
        return 'status-rejected';
      default:
        return '';
    }
  }

  getLowestBid(): BidWithBidder | null {
    if (this.bids.length === 0) return null;
    return this.bids.reduce((prev, curr) => 
      curr.bidAmount < prev.bidAmount ? curr : prev
    );
  }

  getHighestBid(): BidWithBidder | null {
    if (this.bids.length === 0) return null;
    return this.bids.reduce((prev, curr) => 
      curr.bidAmount > prev.bidAmount ? curr : prev
    );
  }

  // Get document paths as array
  getDocumentPaths(bid: BidWithBidder): string[] {
    if (!bid.documentPaths) {
      return bid.documentPath ? [bid.documentPath] : [];
    }
    try {
      // Try to parse as JSON
      const parsed = JSON.parse(bid.documentPaths);
      if (Array.isArray(parsed)) {
        return parsed;
      }
    } catch {
      // If not JSON, try comma-separated
      return bid.documentPaths.split(',').map(p => p.trim()).filter(p => p);
    }
    return [];
  }

  // Check if bid has documents
  hasDocuments(bid: BidWithBidder): boolean {
    return this.getDocumentPaths(bid).length > 0;
  }

  // Download document - downloads the first document or all documents
  downloadDocument(bidId: number, docPath?: string): void {
    if (bidId && docPath) {
      // Download specific document by path
      const fileName = docPath.substring(docPath.lastIndexOf('/') + 1);
      const url = `http://localhost:8080/api/bids/download-document/${bidId}?fileName=${encodeURIComponent(fileName)}`;
      window.open(url, '_blank');
    } else if (bidId) {
      // Fallback: download first document
      const url = `http://localhost:8080/api/bids/download-document/${bidId}`;
      window.open(url, '_blank');
    }
  }

  // Get filename from path
  getFileName(path: string): string {
    if (!path) return 'Document';
    return path.substring(path.lastIndexOf('/') + 1);
  }
}
