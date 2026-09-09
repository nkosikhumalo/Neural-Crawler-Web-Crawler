import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TechTrend {
  id: string;
  canonicalName: string;
  rawName: string;
  category: string;
  source: string;
  sourceUrl: string;
  starCount: number;
  mentionCount: number;
  downloadCount: number;
  snapshotId: string;
  snapshotAt: string;
  tags: string[];
  description?: string;
}

export interface TrendDelta {
  id: string;
  canonicalName: string;
  category: string;
  previousSnapshotId: string;
  currentSnapshotId: string;
  previousStars: number;
  currentStars: number;
  starDelta: number;
  mentionDelta: number;
  growthPercent: number;
  momentum: string;
}

export interface TechSnapshot {
  snapshotId: string;
  triggeredBy: string;
  startedAt: string;
  snapshotAt: string;
  sourcesRan: string[];
  totalItems: number;
  status: string;
  errorNotes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });
  }

  // Get latest snapshot
  getLatestSnapshot(): Observable<any> {
    console.log('Fetching latest snapshot from:', `${this.apiUrl}/snapshot/latest`);
    return this.http.get<any>(`${this.apiUrl}/snapshot/latest`, { headers: this.getHeaders() });
  }

  // Get all trends (latest snapshot)
  getAllTrends(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/trends`, { headers: this.getHeaders() });
  }

  // Get top rising trends
  getTopRising(limit: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/trends/rising?limit=${limit}`, { headers: this.getHeaders() });
  }

  // Get top declining trends
  getTopDeclining(limit: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/trends/declining?limit=${limit}`, { headers: this.getHeaders() });
  }

  // Trigger manual crawl
  triggerCrawl(sources?: string[]): Observable<any> {
    const body = sources ? { sources } : {};
    console.log('Triggering crawl with body:', body);
    return this.http.post<any>(`${this.apiUrl}/crawl/trigger`, body, { headers: this.getHeaders() });
  }

  // Export data as CSV
  exportCsv(snapshotId?: string, includeDeltas: boolean = false): Observable<Blob> {
    let url = `${this.apiUrl}/export/csv?includeDeltas=${includeDeltas}`;
    if (snapshotId) {
      url += `&snapshotId=${snapshotId}`;
    }

    return this.http.get(url, {
      headers: this.getHeaders(),
      responseType: 'blob'
    });
  }

  // Export data as JSON
  exportJson(snapshotId?: string, includeDeltas: boolean = true): Observable<Blob> {
    let url = `${this.apiUrl}/export/json?includeDeltas=${includeDeltas}`;
    if (snapshotId) {
      url += `&snapshotId=${snapshotId}`;
    }

    return this.http.get(url, {
      headers: this.getHeaders(),
      responseType: 'blob'
    });
  }
}
