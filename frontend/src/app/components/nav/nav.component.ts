import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProfileUpdateComponent } from '../profile/profile-update.component';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule, RouterModule, ProfileUpdateComponent],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.css'
})
export class NavComponent {
  username = 'User';
  userId: number | null = null;
  sidebarOpen = false;
  showProfileUpdate = false;
  profileOpen = false;

  constructor() {
    if (typeof localStorage !== 'undefined') {
      this.username = localStorage.getItem('username') || 'User';
      const userIdStr = localStorage.getItem('userId');
      if (userIdStr) {
        this.userId = parseInt(userIdStr, 10);
      }
    }
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar() {
    this.sidebarOpen = false;
  }

  openProfileUpdate() {
    this.showProfileUpdate = true;
    this.sidebarOpen = false;
  }

  closeProfileUpdate() {
    this.showProfileUpdate = false;
  }

  onProfileSave() {
 
    this.username = localStorage.getItem('username') || 'User';
  }

  logout() {
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    window.location.href = '/login';
  }
}
