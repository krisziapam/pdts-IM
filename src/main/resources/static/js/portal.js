// Path: src/main/resources/static/js/portal.js

(function () {
    'use strict';

    // ── Utility ─────────────────────────────────────────────────
    function qs(id) { return document.getElementById(id); }

    function formatDate(iso) {
        if (!iso) return '—';
        const d = new Date(iso);
        return d.toLocaleDateString('en-PH', { month: 'short', day: 'numeric', year: 'numeric' });
    }

    function getQueryParam(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    // ── Show / hide sections ─────────────────────────────────────
    function showContent() {
        qs('portal-loading').style.display = 'none';
        qs('portal-error').style.display   = 'none';
        qs('portal-content').style.display = 'block';
    }

    function showError(msg) {
        qs('portal-loading').style.display = 'none';
        qs('portal-content').style.display = 'none';
        qs('portal-error').style.display   = 'flex';
        const em = qs('portal-error-msg');
        if (em) em.textContent = msg || 'An unexpected error occurred.';
    }

    // ── Render applicant summary ─────────────────────────────────
    function renderSummary(rows) {
        if (!rows || rows.length === 0) return;
        const first = rows[0];

        const name = first.applicantFullName || '—';
        qs('summary-name').textContent     = name;
        qs('summary-initial').textContent  = name.charAt(0).toUpperCase();
        qs('summary-program').textContent  = first.programName || '—';
        qs('summary-ref').textContent      = first.applicationReferenceNumber || '—';
        qs('summary-app-status').textContent = first.applicationStatusName || '—';

        const campusSem = [first.campusName, first.applicationSemester + ' ' + (first.applicationAcademicYear || '')]
            .filter(Boolean).join(' · ');
        qs('summary-campus-sem').textContent = campusSem;
    }

    // ── Render progress bar ──────────────────────────────────────
    function renderProgress(rows) {
        const total    = rows.length;
        const verified = rows.filter(function (r) { return r.requirementStatusName === 'Verified/Received'; }).length;
        const pending  = rows.filter(function (r) { return r.requirementStatusName === 'Pending'; }).length;
        const rejected = rows.filter(function (r) { return r.requirementStatusName === 'Rejected'; }).length;
        const review   = rows.filter(function (r) { return r.requirementStatusName === 'Under Review'; }).length;
        const resub    = rows.filter(function (r) { return r.requirementStatusName === 'For Resubmission'; }).length;

        const pct = total > 0 ? Math.round((verified / total) * 100) : 0;

        qs('progress-pct').textContent   = pct + '%';
        qs('count-verified').textContent = verified + ' Verified';
        qs('count-pending').textContent  = pending  + ' Pending';
        qs('count-rejected').textContent = rejected + ' Rejected';
        qs('count-review').textContent   = review   + ' Under Review';
        qs('count-resub').textContent    = resub    + ' For Resubmission';

        setTimeout(function () {
            qs('progress-fill').style.width = pct + '%';
        }, 100);
    }

    // ── Render documents table ───────────────────────────────────
    function renderDocuments(rows) {
        const tbody = qs('docs-tbody');
        if (!tbody) return;

        if (rows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#aaa;padding:40px;font-style:italic;">No documents found for this application.</td></tr>';
            return;
        }

        tbody.innerHTML = rows.map(function (doc, index) {
            const statusColor  = doc.requirementStatusColor || '#888';
            const statusName   = doc.requirementStatusName  || 'Unknown';
            const uploadDate   = formatDate(doc.requirementUploadDate);
            const processedAt  = formatDate(doc.requirementProcessedAt);

            let notesHtml = '<span style="color:#ccc;">—</span>';

            if (statusName === 'Rejected' && doc.rejectionReasonDescription) {
                notesHtml = '<div class="doc-notes rejection">' +
                    '<strong>Reason:</strong> ' + escapeHtml(doc.rejectionReasonDescription) +
                    '</div>';
            } else if (statusName === 'For Resubmission' && doc.resubmissionNotes) {
                notesHtml = '<div class="doc-notes resubmission">' +
                    '<strong>Guidance:</strong> ' + escapeHtml(doc.resubmissionNotes) +
                    '</div>';
            }

            return '<tr>' +
                '<td><div class="doc-num">' + (index + 1) + '</div></td>' +
                '<td><span class="doc-type-name">' + escapeHtml(doc.requirementTypeName || '—') + '</span></td>' +
                '<td><span class="doc-tracking">' + escapeHtml(doc.requirementTrackingNo || '—') + '</span></td>' +
                '<td>' +
                    '<span class="doc-status-badge" style="background:' + statusColor + '22;color:' + statusColor + ';border:1.5px solid ' + statusColor + '55;">' +
                        escapeHtml(statusName) +
                    '</span>' +
                '</td>' +
                '<td class="doc-date">' + uploadDate + '</td>' +
                '<td class="doc-date">' + processedAt + '</td>' +
                '<td>' + notesHtml + '</td>' +
            '</tr>';
        }).join('');
    }

    // ── HTML escape ──────────────────────────────────────────────
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // ── Load portal data ─────────────────────────────────────────
    async function loadPortalData() {
        const refNo = getQueryParam('ref');
        const token = getQueryParam('token');

        if (!refNo || !token) {
            showError('Missing reference number or access token. Return to the portal and try again.');
            return;
        }

        try {
            const resp = await fetch('/api/portal/status?' +
                new URLSearchParams({ ref: refNo, token: token }));

            if (!resp.ok) {
                const data = await resp.json().catch(function () { return {}; });
                showError(data.error || 'Access denied. Please check your reference number and token.');
                return;
            }

            const rows = await resp.json();

            renderSummary(rows);
            renderProgress(rows);
            renderDocuments(rows);
            showContent();

        } catch (err) {
            showError('Could not connect to the server. Please check your internet connection.');
        }
    }

    // ── Footer year ──────────────────────────────────────────────
    function setFooterYear() {
        const el = qs('footer-year');
        if (el) el.textContent = new Date().getFullYear();
    }

    // ── Init ────────────────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', function () {
        setFooterYear();
        loadPortalData();
    });

}());
