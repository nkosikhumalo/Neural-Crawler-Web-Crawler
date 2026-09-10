import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  latestSnapshot: any = null;
  trends: any[] = [];
  risingTrends: any[] = [];
  decliningTrends: any[] = [];
  newsTrends: any[] = [];
  jobTrends: any[] = [];
  languageTrends: any[] = [];
  loading = true;
  error: string | null = null;
  isCrawling = false;
  sourceSummary: { name: string; count: number }[] = [];

  get githubCount(): number {
    return this.trends.filter(t => t.source === 'GITHUB').length;
  }
  get hnCount(): number {
    return this.trends.filter(t => t.source === 'HACKERNEWS_API' || t.source === 'HACKERNEWS').length;
  }
  get mavenCount(): number {
    return this.trends.filter(t => t.source === 'MAVEN_CENTRAL').length;
  }
  get soCount(): number {
    return this.trends.filter(t => t.source === 'STACKOVERFLOW_JOBS').length;
  }

  get featuredNews(): any | null {
    return this.newsTrends[0] || null;
  }

  get lastCrawlTime(): string {
    return this.latestSnapshot?.snapshotAt || this.latestSnapshot?.startedAt || '';
  }

  private pollInterval: any = null;

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.clearPolling();
  }

  loadDashboardData(): void {
    this.loading = true;
    this.error = null;

    this.apiService.getLatestSnapshot().subscribe({
      next: (data: any) => {
        this.latestSnapshot = data;
        this.trends = data?.trends || [];
        this.risingTrends = data?.topRising || [];
        this.decliningTrends = data?.topDeclining || [];
        this.buildContentSections();
        this.buildSourceSummary();
        this.loading = false;
      },
      error: (err: any) => {
        this.loading = false;
        // 404 means no snapshot yet — show empty state, not an error
        if (err.status === 404 || err.status === 0) {
          this.latestSnapshot = null;
          this.trends = [];
          this.risingTrends = [];
          this.decliningTrends = [];
        } else {
          this.error = 'Could not reach the backend. Make sure the server is running on port 8080.';
          console.error('Error loading snapshot:', err);
        }
      }
    });
  }

  triggerCrawl(): void {
    if (this.isCrawling) return;
    this.isCrawling = true;
    this.error = null;
    const previousSnapshotId = this.latestSnapshot?.snapshotId || null;

    this.apiService.triggerCrawl().subscribe({
      next: (response: any) => {
        console.log('Crawl triggered:', response);
        // Poll every 5 seconds until we get fresh data
        this.startPolling(previousSnapshotId);
      },
      error: (err: any) => {
        // 409 means already running — still poll for the result
        if (err.status === 409) {
          console.log('Crawl already in progress, polling for data...');
          this.startPolling(previousSnapshotId);
        } else {
          this.error = 'Failed to trigger crawl: ' + (err.error?.message || err.message || 'Unknown error');
          this.isCrawling = false;
        }
      }
    });
  }

  private startPolling(previousSnapshotId: string | null): void {
    this.clearPolling();
    let attempts = 0;
    const maxAttempts = 36; // 3 minutes max at 5s intervals

    this.pollInterval = setInterval(() => {
      attempts++;
      this.apiService.getLatestSnapshot().subscribe({
        next: (data: any) => {
          // Always update what we have, even if still running
          if (data) {
            this.latestSnapshot = data;
            this.trends = data?.trends || [];
            this.risingTrends = data?.topRising || [];
            this.decliningTrends = data?.topDeclining || [];
              this.buildContentSections();
            this.buildSourceSummary();
          }
          // Stop polling once truly complete
          const isNewSnapshot = !previousSnapshotId || data?.snapshotId !== previousSnapshotId;
          if (data && isNewSnapshot
            && (data.status === 'COMPLETED' || data.status === 'PARTIAL' || data.status === 'FAILED')) {
            this.isCrawling = false;
            this.clearPolling();
          }
        },
        error: () => { }
      });

      if (attempts >= maxAttempts) {
        this.isCrawling = false;
        this.clearPolling();
        this.loadDashboardData();
      }
    }, 5000);
  }

  private clearPolling(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  exportCsv(): void {
    if (!this.latestSnapshot) return;

    this.apiService.exportCsv(this.latestSnapshot.snapshotId).subscribe({
      next: (blob: Blob) => {
        this.downloadFile(blob, `radar-${this.latestSnapshot.snapshotId}.csv`, 'text/csv');
      },
      error: (err: any) => {
        this.error = 'Failed to export CSV: ' + (err.message || 'Unknown error');
        console.error('Error exporting CSV:', err);
      }
    });
  }

  exportJson(): void {
    if (!this.latestSnapshot) return;

    this.apiService.exportJson(this.latestSnapshot.snapshotId, true).subscribe({
      next: (blob: Blob) => {
        this.downloadFile(blob, `radar-${this.latestSnapshot.snapshotId}.json`, 'application/json');
      },
      error: (err: any) => {
        this.error = 'Failed to export JSON: ' + (err.message || 'Unknown error');
        console.error('Error exporting JSON:', err);
      }
    });
  }

  private downloadFile(blob: Blob, filename: string, mimeType: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  getStatusClass(): string {
    if (!this.latestSnapshot?.status) return 'unknown';
    return this.latestSnapshot.status.toLowerCase();
  }

  getMomentumClass(momentum: string): string {
    if (!momentum) return '';
    switch (momentum.toLowerCase()) {
      case 'rising':
        return 'momentum-rising';
      case 'declining':
        return 'momentum-declining';
      case 'stable':
        return 'momentum-stable';
      case 'new':
        return 'momentum-new';
      default:
        return '';
    }
  }

  formatGrowthPercent(value: number | null): string {
    if (value === null || value === undefined) return 'N/A';
    const sign = value > 0 ? '+' : '';
    const decimals = Math.abs(value) > 0 && Math.abs(value) < 0.1 ? 3 : 1;
    return `${sign}${value.toFixed(decimals)}%`;
  }

  formatSourceName(source: string): string {
    if (!source) return '—';
    const map: Record<string, string> = {
      'GITHUB': 'GitHub',
      'HACKERNEWS': 'HackerNews',
      'HACKERNEWS_API': 'HackerNews',
      'MAVEN_CENTRAL': 'Maven',
      'STACKOVERFLOW_JOBS': 'SO Jobs'
    };
    return map[source.toUpperCase()] || source;
  }

  private buildSourceSummary(): void {
    const counts: Record<string, number> = {};
    for (const t of this.trends) {
      const src = t.source || 'UNKNOWN';
      counts[src] = (counts[src] || 0) + 1;
    }
    for (const source of this.latestSnapshot?.sourcesRan || []) {
      if (!(source in counts)) counts[source] = 0;
    }
    this.sourceSummary = Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }

  private buildContentSections(): void {
    const orderedTrends = [...this.trends].sort((a, b) => {
      const dateA = new Date(a.snapshotAt || 0).getTime();
      const dateB = new Date(b.snapshotAt || 0).getTime();
      return dateB - dateA;
    });

    this.newsTrends = orderedTrends
      .filter(trend => trend.source === 'HACKERNEWS' || trend.source === 'HACKERNEWS_API')
      .slice(0, 50);
    this.jobTrends = orderedTrends
      .filter(trend => trend.source === 'STACKOVERFLOW_JOBS')
      .slice(0, 10);
    this.languageTrends = orderedTrends
      .filter(trend => trend.category === 'LANGUAGE' || (trend.source === 'GITHUB' && trend.tags?.length))
      .slice(0, 10);
  }
}
