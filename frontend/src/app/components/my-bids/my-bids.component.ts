import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BidWithTender } from '../../models/bidder.model';

@Component({
  selector: 'app-my-bids',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-bids.component.html',
  styleUrl: './my-bids.component.css'
})
export class MyBidsComponent {
  @Input() myBids: BidWithTender[] = [];
  @Input() loadingMyBids: boolean = false;
  @Input() showMyBids: boolean = false;
  @Output() toggleMyBids = new EventEmitter<void>();

  onToggle() {
    this.toggleMyBids.emit();
  }
}
