import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard-actions',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-actions.component.html',
  styleUrl: './dashboard-actions.component.css'
})
export class DashboardActionsComponent {
}
