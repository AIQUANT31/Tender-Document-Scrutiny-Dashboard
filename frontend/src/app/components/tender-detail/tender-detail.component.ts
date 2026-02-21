import { Component, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TenderBidsComponent } from '../tender-bids/tender-bids.component';

interface TenderDetail {
  id: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string | null;
  updatedAt: string | null;
  status: string;
  budget: number;
  deadline?: string | null;
  openingDate?: string | null;
  requiredDocuments?: string;
  category?: string;
  location?: string | null;
  contactNumber?: string | null;
  isWhatsapp?: boolean;
  comments?: string | null;
  userType?: string;
}

interface Bid {
  id: number;
  tenderId: number;
  bidderId: number;
  bidAmount: number;
  proposalText: string;
  status: string;
  createdAt: string | null;
}

interface BidderProfile {
  id: number;
  companyName: string;
  email: string;
  phone: string;
  type: string;
  status: string;
}

@Component({
  selector: 'app-tender-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, TenderBidsComponent],
  templateUrl: './tender-detail.component.html',
  styleUrl: './tender-detail.component.css'
})
export class TenderDetailComponent {
  
  @Input() set tender(value: TenderDetail | null) {
    this._tender = value;
    if (value) {
      this.loadBidCount();
    }
  }

  get tender(): TenderDetail | null {
    return this._tender;
  }

  private _tender: TenderDetail | null = null;
  @Input() currentUserId: number | null = null;
  @Output() onClose = new EventEmitter<void>();
  @Output() onDelete = new EventEmitter<number>();
  @Output() onUpdate = new EventEmitter<any>();
  
  showBidForm = false;
  bidAmount = '';
  proposalText = '';
  contactNumber = '';
  bidError = '';
  existingBids: Bid[] = [];
  loadingBids = false;
  hasExistingBid = false;
  
  // View bids modal
  showBids = false;
  bidCount = 0;
  
  // Current user's bidder profiles
  bidderProfiles: BidderProfile[] = [];
  loadingProfiles = false;
  
  // Current user's bidder ID (stored in localStorage)
  bidderId: number | null = null;

  // Load bid count for the tender
  loadBidCount() {
    if (!this.tender) return;
    
    this.http.get<any>(`http://localhost:8080/api/bids/tender/${this.tender.id}/stats`).subscribe({
      next: (stats) => {
        this.bidCount = stats.totalBids || 0;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading bid count:', error);
        this.bidCount = 0;
      }
    });
    
  }

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private router: Router) {
    // Try to get bidderId from localStorage
    const bidderIdStr = localStorage.getItem('bidderId');
    if (bidderIdStr) {
      this.bidderId = parseInt(bidderIdStr, 10);
    }
    
    // Try to get userId from localStorage if not provided as input
    const userIdStr = localStorage.getItem('userId');
    if (userIdStr) {
      this.currentUserId = parseInt(userIdStr, 10);
    }
  }

  close() {
    this.showBidForm = false;
    this.showBids = false;
    this.onClose.emit();
  }

  // Open bids view for tender creator
  openBids() {
    console.log('Opening called tender', this.tender?.id);
    this.showBids = true;
    // Reload bids when opening
    this.cdr.detectChanges();
     console.log('showBids set to:', this.showBids);
  }

  closeBids() {
    this.showBids = false;
    this.cdr.detectChanges();
  }

  deleteTender() {
    if (this.tender && this.currentUserId && this.tender.createdBy === this.currentUserId) {
      if (confirm('Are you sure you want to delete this tender?')) {
        this.onDelete.emit(this.tender.id);
        this.close();
      }
    }
  }

  isCreator(): boolean {
    return this.currentUserId !== null && 
           this.tender !== null && 
           this.tender.createdBy === this.currentUserId;
  }

  // Check if user can bid (not the creator and tender is open)
  canBid(): boolean {
    if (!this.tender) return false;
    
    // Check if tender is closed based on deadline
    const isDeadlinePassed = this.isTenderDeadlinePassed();
    
    return !isDeadlinePassed && 
           this.tender.status === 'OPEN' && 
           this.currentUserId !== null && 
           this.tender.createdBy !== this.currentUserId;
  }

  // Check if tender deadline has passed
  isTenderDeadlinePassed(): boolean {
    if (!this.tender?.deadline) return false;
    const deadlineDate = new Date(this.tender.deadline);
    const now = new Date();
    now.setHours(23, 59, 59, 999);
    return deadlineDate < now;
  }

  // Get the actual tender status considering deadline
  getTenderStatus(): string {
    if (!this.tender) return '';
    if (this.tender.status === 'CLOSED') return 'CLOSED';
    if (this.isTenderDeadlinePassed()) return 'CLOSED';
    return this.tender.status;
  }

  // Open bid form - redirect to new page
  openBidForm() {
    if (this.tender) {
      this.router.navigate(['/place-bid', this.tender.id]);
    }
  }

  // Load bidder profiles for the current user
  loadBidderProfiles() {
    if (!this.currentUserId) return;
    
    this.loadingProfiles = true;
    this.http.get<BidderProfile[]>(`http://localhost:8080/api/bidders/user/${this.currentUserId}`).subscribe({
      next: (profiles) => {
        this.bidderProfiles = profiles;
        // If user has a profile, set it as the selected bidder
        if (profiles.length > 0) {
          this.bidderId = profiles[0].id;
          localStorage.setItem('bidderId', this.bidderId.toString());
        }
        this.loadingProfiles = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading bidder profiles:', error);
        this.loadingProfiles = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeBidForm() {
    this.showBidForm = false;
  }

  // Check if user already has a bid on this tender
  checkExistingBid() {
    if (!this.tender || !this.bidderId) return;
    
    this.loadingBids = true;
    this.http.get<Bid[]>(`http://localhost:8080/api/bids/tender/${this.tender.id}`).subscribe({
      next: (bids) => {
        this.existingBids = bids;
        this.hasExistingBid = bids.some(b => b.bidderId === this.bidderId);
        this.loadingBids = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error checking bids:', error);
        this.loadingBids = false;
        this.cdr.detectChanges();
      }
    });
  }

  submitBid() {
    if (!this.tender) return;

    if (!this.bidAmount || parseFloat(this.bidAmount) <= 0) {
      this.bidError = 'Please enter a valid bid amount';
      return;
    }

    if (!this.bidderId) {
      this.bidError = 'Please select a bidder profile first';
      return;
    }

    const bidRequest = {
      tenderId: this.tender.id,
      bidderId: this.bidderId,
      bidAmount: parseFloat(this.bidAmount),
      proposalText: this.proposalText,
      contactNumber: this.contactNumber,
      status: 'PENDING'
    };

    this.http.post<any>('http://localhost:8080/api/bids/create', bidRequest).subscribe({
      next: (response) => {
        if (response.success) {
          alert('Bid placed successfully!');
          this.showBidForm = false;
          this.hasExistingBid = true;
          // Reload bids
          this.checkExistingBid();
        } else {
          this.bidError = response.message || 'Error placing bid';
        }
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error placing bid:', error);
        this.bidError = 'Error placing bid. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  getRequiredDocumentsList(): string[] {
    if (this.tender?.requiredDocuments) {
      return this.tender.requiredDocuments.split(',').map(doc => doc.trim()).filter(doc => doc);
    }
    return [];
  }

  getCategoryDisplayName(): string {
    if (!this.tender?.category) return 'Not specified';
    const categoryNames: { [key: string]: string } = {
      'construction': 'Construction',
      'infrastructure': 'Infrastructure',
      'engineering': 'Engineering',
      'renovation': 'Renovation',
      'maintenance': 'Maintenance'
    };
    return categoryNames[this.tender.category] || this.tender.category;
  }
}
