import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NgIf, isPlatformBrowser } from '@angular/common';
import { NavComponent } from '../../components/nav/nav.component';
import { BidderHeaderComponent } from '../../components/bidder-header/bidder-header.component';
import { BidderStatsComponent } from '../../components/bidder-stats/bidder-stats.component';
import { BidderTableComponent } from '../../components/bidder-table/bidder-table.component';
import { MyBidsComponent } from '../../components/my-bids/my-bids.component';
import { BidderFormComponent } from '../../components/bidder-form/bidder-form.component';
import { BidderDetailComponent } from '../../components/bidder-detail/bidder-detail.component';
import { BidderData, BidWithTender, BidderFormData, DEFAULT_BIDDER_FORM_DATA } from '../../models/bidder.model';

@Component({
  selector: 'app-bidder',
  standalone: true,
  imports: [NgIf, NavComponent, BidderHeaderComponent, BidderStatsComponent, BidderTableComponent, MyBidsComponent, BidderFormComponent, BidderDetailComponent],
  templateUrl: './bidder.html',
  styleUrl: './bidder.css'
})
export class Bidder implements OnInit {
  userId = 0;
  bidders: BidderData[] = [];
  myBids: BidWithTender[] = [];
  loading = true; // Start with loading true
  loadingMyBids = false;
  showBidderForm = false;
  showBidderDetail = false;
  showMyBids = false;
  isEditing = false;
  selectedBidder: BidderData | null = null;
  selectedBidderId: number | null = null;
  formData: BidderFormData = { ...DEFAULT_BIDDER_FORM_DATA };

  constructor(
    private http: HttpClient, 
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    
    if (isPlatformBrowser(this.platformId)) {
      
      const storedUserId = localStorage.getItem('userId');
      this.userId = storedUserId ? parseInt(storedUserId, 10) : 0;
      console.log('Bidder page loaded, userId:', this.userId);
    }
    this.loadBidders();
  }

  loadBidders() {
    console.log('loadBidders called, userId:', this.userId);
    if (!this.userId) {
      console.log('No userId, cannot load bidders');
      this.loading = false;
      return;
    }
    this.loading = true;
    console.log('Fetching bidders from API...');
    
    
    setTimeout(() => {
      this.http.get<BidderData[]>(`http://localhost:8080/api/bidders/user/${this.userId}`).subscribe({
        next: (data) => {
          console.log('Bidders loaded:', data);
          this.bidders = data.map(b => ({ ...b, createdAt: this.toDate(b.createdAt), updatedAt: this.toDate(b.updatedAt) }));
          this.loading = false;
          if (this.bidders.length) this.loadMyBids();
        },
        error: (err) => {
          console.error('Error loading bidders:', err);
          this.loading = false;
        }
      });
    }, 0);
  }

  loadMyBids() {
    this.loadingMyBids = true;
    this.http.get<BidWithTender[]>(`http://localhost:8080/api/bids/bidder/${this.bidders[0].id}/with-tenders`).subscribe({
      next: (bids) => { this.myBids = bids; this.loadingMyBids = false; this.cdr.detectChanges(); },
      error: () => { this.myBids = []; this.loadingMyBids = false; this.cdr.detectChanges(); }
    });
  }

  toggleMyBids() { this.showMyBids = !this.showMyBids; if (this.showMyBids && !this.myBids.length) this.loadMyBids(); }

  private toDate(v: any): string | null {
    if (!v) return null;
    if (Array.isArray(v) && v.length >= 6) return new Date(v[0], v[1] - 1, v[2], v[3], v[4], v[5]).toISOString();
    const s = String(v);
    if (s.includes('T') || s.includes('-')) return s;
    const p = s.split(',');
    return p.length >= 6 ? new Date(+p[0], +p[1] - 1, +p[2], +p[3], +p[4], +p[5]).toISOString() : s;
  }

  
  addBidder() { this.isEditing = false; this.selectedBidder = null; this.formData = { ...DEFAULT_BIDDER_FORM_DATA }; this.showBidderForm = true; }
  editBidder(b: BidderData) { this.isEditing = true; this.selectedBidder = b; this.formData = { companyName: b.companyName, email: b.email, phone: b.phone || '', type: b.type || 'Construction', address: b.address || '', contactPerson: b.contactPerson || '', status: b.status }; this.showBidderForm = true; }
  onBidderSubmit(f: BidderFormData) { this.isEditing && this.selectedBidder ? this.updateBidder(f) : this.createBidder(f); }
  onBidderCancel() { this.showBidderForm = false; this.selectedBidder = null; this.isEditing = false; }

  private createBidder(f: BidderFormData) {
    if (!this.userId) return;
    this.http.post('http://localhost:8080/api/bidders/create', { ...f, createdBy: this.userId, totalBids: 0, winningBids: 0 }).subscribe({
      next: (r: any) => { if (r.success) { this.showBidderForm = false; if (typeof localStorage !== 'undefined' && r.bidder?.id) localStorage.setItem('bidderId', r.bidder.id); this.loadBidders(); } },
      error: () => alert('Error creating bidder')
    });
  }

  private updateBidder(f: BidderFormData) {
    if (!this.selectedBidder || !this.userId) return;
    this.http.put(`http://localhost:8080/api/bidders/${this.selectedBidder.id}?userId=${this.userId}`, f).subscribe({
      next: (r: any) => { if (r.success) { this.showBidderForm = false; this.loadBidders(); } },
      error: (e: any) => alert(e.status === 403 ? 'Not authorized' : ' updating bidder')
    });
  }



  viewBidderDetails(id: number) { this.selectedBidderId = id; this.showBidderDetail = true; }
  onDeleteSuccess() { this.loadBidders(); }
  onError(msg: string) { alert(msg); }
  closeBidderDetail() { this.showBidderDetail = false; this.selectedBidderId = null; }
}
