import { Routes } from '@angular/router';

export const routes: Routes = [
  
    {path : '', redirectTo: 'login', pathMatch: 'full'},
  
    {
        path: 'login',
        loadComponent: () => import('./pages/login/login').then(m => m.Login)
    },
    
    {
        path: 'signup',
        loadComponent: () => import('./pages/signup/signup').then(m => m.Signup)
    },
    
    {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard)
    },

    {
        path: 'tender',
        loadComponent: () => import('./pages/tender/tender').then(m => m.TenderPage)
    },
   
    {
        path: 'bidder',
        loadComponent: () => import('./pages/bidder/bidder').then(m => m.Bidder)
    },
    
    {
        path: 'place-bid/:id',
        loadComponent: () => import('./pages/place-bid/place-bid.component').then(m => m.PlaceBidComponent)
    },
    // Default route redirects to dashboard after login
    {path : '', redirectTo: 'dashboard', pathMatch: 'full'}
    
];
