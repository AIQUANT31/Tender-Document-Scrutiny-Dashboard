import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = '';
  password = '';
  message = '';
  isSuccess = false;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login() {
    const loginData = {
      username: this.username,
      password: this.password
    };

    this.http.post<any>('http://localhost:8080/api/auth/login', loginData)
      .subscribe({
        next: (response: any) => {
          this.message = response.message;
          this.isSuccess = response.success === true;
          if (this.isSuccess) {
            localStorage.setItem('username', this.username);
            if (response.user && response.user.id) {
              localStorage.setItem('userId', response.user.id.toString());
            }
            this.router.navigate(['/dashboard']); // Redirect to dashboard page after login
          } else {
            alert(this.message);
          }
        },
        error: (error) => {
          const backendMessage =
            error?.error?.message ||
            error?.error?.error ||
            error?.message ||
            'Unknown error';
          this.message = 'Login failed: ' + backendMessage;
          this.isSuccess = false;
          alert(this.message);
        }
      });
  }
}
