import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface ProfileData {
  username: string;
  email: string;
  password?: string;
}

interface ApiResponse {
  success: boolean;
  message: string;
  user?: {
    id: number;
    username: string;
    email: string;
  };
}

@Component({
  selector: 'app-profile-update',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-update.component.html',
  styleUrl: './profile-update.component.css'
})
export class ProfileUpdateComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() userId: number | null = null;
  @Output() onCancel = new EventEmitter<void>();
  @Output() onSave = new EventEmitter<void>();

  formData: ProfileData = {
    username: '',
    email: '',
    password: ''
  };

  loading = false;
  errorMessage = '';
  successMessage = '';
  profileLoaded = false;

  private readonly API_BASE_URL = 'http://localhost:8080/api/auth';
  private cachedUserId: number | null = null;
  private platformId = inject(PLATFORM_ID);

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadUserFromStorage();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && changes['isOpen'].currentValue === true) {
      this.errorMessage = '';
      this.successMessage = '';
      this.formData.password = '';
      this.loadProfile();
    }
  }

  private loadUserFromStorage(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const userIdStr = localStorage.getItem('userId');
    if (userIdStr) {
      this.cachedUserId = parseInt(userIdStr, 10);
      this.loadProfile();
    }
  }

  loadProfile(): void {
    let currentUserId = this.userId || this.cachedUserId;
    
    if (!currentUserId) {
      if (isPlatformBrowser(this.platformId)) {
        const userIdStr = localStorage.getItem('userId');
        if (userIdStr) {
          currentUserId = parseInt(userIdStr, 10);
        }
      }
    }

    if (!currentUserId) {
      this.errorMessage = 'User not logged in';
      return;
    }

    this.cachedUserId = currentUserId;
    
    // If profile is already loaded and modal is just reopening, don't reload
    if (this.profileLoaded && this.formData.username && this.formData.email) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.http.get<ApiResponse>(`${this.API_BASE_URL}/profile/${currentUserId}`).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success && response.user) {
          this.formData.username = response.user.username;
          this.formData.email = response.user.email;
          this.profileLoaded = true;
        } else {
          this.errorMessage = response.message || 'Failed to load profile';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Error loading profile. Please try again.';
        console.error('Error loading profile:', error);
      }
    });
  }

  onSubmit(): void {
    let currentUserId = this.userId || this.cachedUserId;
    
    if (!currentUserId) {
      if (isPlatformBrowser(this.platformId)) {
        const userIdStr = localStorage.getItem('userId');
        if (userIdStr) {
          currentUserId = parseInt(userIdStr, 10);
        }
      }
    }

    if (!currentUserId) {
      this.errorMessage = 'User not logged in';
      return;
    }

    if (!this.formData.username || !this.formData.email) {
      this.errorMessage = 'Username and email are required';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: ProfileData & { userId: number } = {
      userId: currentUserId,
      username: this.formData.username,
      email: this.formData.email
    };

    if (this.formData.password && this.formData.password.trim() !== '') {
      request.password = this.formData.password;
    }

    this.http.put<ApiResponse>(`${this.API_BASE_URL}/profile`, request).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.successMessage = response.message || 'Profile updated successfully!';
          if (response.user && isPlatformBrowser(this.platformId)) {
            localStorage.setItem('username', response.user.username);
            localStorage.setItem('email', response.user.email);
          }
          this.formData.password = '';
          
          setTimeout(() => {
            this.onSave.emit();
            this.onCancel.emit();
          }, 1000);
        } else {
          this.errorMessage = response.message || 'Failed to update profile';
        }
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = 'Error updating profile. Please try again.';
        console.error('Error updating profile:', error);
      }
    });
  }

  closeModal(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.formData.password = '';
    this.onCancel.emit();
  }

  clearError(): void {
    this.errorMessage = '';
  }
}
