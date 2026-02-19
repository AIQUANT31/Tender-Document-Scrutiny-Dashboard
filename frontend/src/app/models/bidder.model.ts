// Shared models for Bidder functionality

export interface BidderData {
  id: number;
  companyName: string;
  email: string;
  phone: string;
  type: string;
  totalBids: number;
  winningBids: number;
  status: string;
  createdBy: number;
  createdAt: string | null;
  updatedAt: string | null;
  address: string;
  contactPerson: string;
}

export interface BidWithTender {
  bidId: number;
  tenderId: number;
  tenderName: string;
  tenderDescription: string;
  tenderBudget: number;
  tenderStatus: string;
  tenderDeadline: string;
  tenderLocation: string;
  bidderId: number;
  bidAmount: number;
  proposalText: string;
  status: string;
  createdAt: string;
  contactNumber: string;
  hasDocument: boolean;
}

export interface BidderFormData {
  companyName: string;
  email: string;
  phone: string;
  type: string;
  address: string;
  contactPerson: string;
  status: string;
}

export const DEFAULT_BIDDER_FORM_DATA: BidderFormData = {
  companyName: '',
  email: '',
  phone: '',
  type: 'Construction',
  address: '',
  contactPerson: '',
  status: 'ACTIVE'
};
