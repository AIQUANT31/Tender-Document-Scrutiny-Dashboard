import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BidderData } from '../../models/bidder.model';

@Component({
  selector: 'app-bidder-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bidder-header.component.html',
  styleUrl: './bidder-header.component.css'
})
export class BidderHeaderComponent {
  @Input() bidders: BidderData[] = [];
  @Output() addBidder = new EventEmitter<void>();

  // Computed property - check if can add new bidder
  get canAddBidder(): boolean {
    return this.bidders.length === 0;
  }

  // Get first bidder's company name
  get companyName(): string {
    return this.bidders[0]?.companyName || '';
  }

  onAddBidder() {
    if (this.bidders.length > 0) {
      alert('You can only create one bidder profile. Please edit your existing profile instead.');
      return;
    }
    this.addBidder.emit();
  }
}
