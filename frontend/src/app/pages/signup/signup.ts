import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-signup',
  imports: [FormsModule, RouterModule, CommonModule],
  templateUrl: './signup.html',
  styleUrl: './signup.css',
})
export class Signup {
  username = '';
  email = '';
  password = '';
  message = '';
  isSuccess = false;

  constructor(private http: HttpClient, private router: Router) {}

  signup() {
  
    if (!this.username || !this.email || !this.password) {
      this.message = 'Please fill in all fields';
      this.isSuccess = false;
      alert(this.message);
      return;
    }

    const signupData = {
      username: this.username,
      email: this.email,
      password: this.password
    };
 console.log('Sending signup request to: http://localhost:8080/api/auth/signup');

    this.http.post<{ message: string }>('http://localhost:8080/api/auth/signup', signupData)
      .subscribe({
        next: (response) => {
             console.log('Signup response:', response);
          this.message = response.message;
          this.isSuccess = response.message === 'User registered successfully!';
          if (this.isSuccess) {
            alert('Signup successful! Redirecting to login...');
            
            this.username = '';
            this.email = '';
            this.password = '';
          
            setTimeout(() => {
              this.router.navigate(['/login']);
            }, 1500);
          }
        },
        error: (error) => {
           console.error('Signup error details:', error);
          let backendMessage = 'Unknown error';
          
          if (error.status === 0) {
            backendMessage = 'Cannot connect to server. Make sure backend is running on http://localhost:8080 , Restart the backend server and try again.';
          } else if (error.error && typeof error.error === 'object') {
            backendMessage = error.error.message || error.error.error || JSON.stringify(error.error);
          } else if (error.message) {
            backendMessage = error.message;
          }
          
          this.message = 'Signup failed: ' + backendMessage;
          this.isSuccess = false;
          alert(this.message);
        }
      });
  }
}
