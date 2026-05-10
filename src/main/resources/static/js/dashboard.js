// Path: src/main/resources/static/js/dashboard.js

(function () {
    'use strict';

    // ── Date display ────────────────────────────────────────────
    function renderDate() {
        const el = document.getElementById('current-date');
        if (!el) return;
        const now = new Date();
        el.textContent = now.toLocaleDateString('en-PH', {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
        });
    }

    // ── Toast notifications ─────────────────────────────────────
    function toast(message, type) {
        type = type || 'success';

        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.style.cssText = [
                'position:fixed', 'bottom:28px', 'right:28px',
                'display:flex', 'flex-direction:column', 'gap:10px',
                'z-index:9999', 'pointer-events:none'
            ].join(';');
            document.body.appendChild(container);
        }

        const colors = {
            success: { bg: '#e8f5e9', border: '#66bb6a', text: '#2e7d32', icon: '✓' },
            error:   { bg: '#ffebee', border: '#ef5350', text: '#c62828', icon: '✕' },
            info:    { bg: '#e3f2fd', border: '#42a5f5', text: '#1565c0', icon: 'ℹ' },
            warning: { bg: '#fff8e1', border: '#ffca28', text: '#f57f17', icon: '⚠' }
        };

        const c = colors[type] || colors.info;
        const el = document.createElement('div');
        el.style.cssText = [
            'display:flex', 'align-items:center', 'gap:10px',
            `background:${c.bg}`, `border:1.5px solid ${c.border}`,
            `color:${c.text}`, 'border-radius:10px',
            'padding:12px 18px', 'font-size:.84rem', 'font-weight:500',
            'box-shadow:0 4px 16px rgba(0,0,0,.12)',
            'pointer-events:auto', 'min-width:260px', 'max-width:380px',
            'animation:toastIn .3s cubic-bezier(.4,0,.2,1) both',
            'font-family:DM Sans,sans-serif'
        ].join(';');
        el.innerHTML = `<span style="font-size:1rem;font-weight:700;">${c.icon}</span><span>${message}</span>`;

        if (!document.getElementById('toast-keyframes')) {
            const style = document.createElement('style');
            style.id = 'toast-keyframes';
            style.textContent = `
                @keyframes toastIn  { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:none; } }
                @keyframes toastOut { from { opacity:1; } to { opacity:0; transform:translateY(8px); } }
            `;
            document.head.appendChild(style);
        }

        container.appendChild(el);
        setTimeout(function () {
            el.style.animation = 'toastOut .3s ease forwards';
            setTimeout(function () { el.remove(); }, 320);
        }, 3500);
    }

    // ── Document status action modal ────────────────────────────
    function buildModal() {
        if (document.getElementById('action-modal')) return;

        const overlay = document.createElement('div');
        overlay.id = 'action-modal';
        overlay.style.cssText = [
            'display:none', 'position:fixed', 'inset:0',
            'background:rgba(0,0,0,.45)', 'backdrop-filter:blur(4px)',
            'z-index:500', 'align-items:center', 'justify-content:center',
            'animation:fadeIn .2s ease'
        ].join(';');

        overlay.innerHTML = `
          <div id="modal-box" style="
            background:#fff; border-radius:16px; padding:32px; width:100%; max-width:460px;
            box-shadow:0 24px 64px rgba(0,0,0,.2); margin:16px; position:relative;
            animation:slideUp .25s cubic-bezier(.4,0,.2,1);">

            <button onclick="closeModal()" style="
              position:absolute; top:16px; right:16px; background:none; border:none;
              cursor:pointer; color:#888; font-size:1.2rem; line-height:1; padding:4px 8px;
              border-radius:6px;">✕</button>

            <h3 id="modal-title" style="
              font-family:DM Sans,sans-serif; font-size:1.15rem; font-weight:700;
              color:#1a1a2e; margin-bottom:6px;">Update Document Status</h3>
            <p id="modal-doc-info" style="font-size:.82rem; color:#7a7a8a; margin-bottom:24px;"></p>

            <!-- Rejection reason (shown only for reject action) -->
            <div id="rejection-field" style="display:none; margin-bottom:20px;">
              <label style="font-size:.78rem; font-weight:700; color:#4a4a5a; display:block; margin-bottom:6px; text-transform:uppercase; letter-spacing:.05em;">
                Rejection Reason *
              </label>
              <select id="rejection-reason-select" style="
                width:100%; height:42px; padding:0 12px; border:1.5px solid #e8e5e0;
                border-radius:8px; font-family:DM Sans,sans-serif; font-size:.85rem;
                color:#1a1a2e; background:#faf8f5; outline:none;">
                <option value="">— Select a reason —</option>
              </select>
            </div>

            <!-- Remarks (shown for resubmission) -->
            <div id="remarks-field" style="display:none; margin-bottom:20px;">
              <label style="font-size:.78rem; font-weight:700; color:#4a4a5a; display:block; margin-bottom:6px; text-transform:uppercase; letter-spacing:.05em;">
                Guidance Notes for Student
              </label>
              <textarea id="remarks-input" rows="3" placeholder="Explain what needs to be corrected…"
                style="width:100%; padding:10px 12px; border:1.5px solid #e8e5e0; border-radius:8px;
                font-family:DM Sans,sans-serif; font-size:.85rem; resize:vertical; outline:none;"></textarea>
            </div>

            <div style="display:flex; gap:10px; justify-content:flex-end; flex-wrap:wrap;">
              <button onclick="closeModal()" style="
                padding:10px 22px; border:1.5px solid #e8e5e0; background:transparent;
                border-radius:8px; font-family:DM Sans,sans-serif; font-size:.85rem;
                cursor:pointer; color:#4a4a5a; transition:all .2s;">Cancel</button>
              <button id="modal-confirm-btn" style="
                padding:10px 24px; background:#7B0D1E; color:#fff; border:none;
                border-radius:8px; font-family:DM Sans,sans-serif; font-size:.85rem;
                font-weight:600; cursor:pointer; transition:all .2s;
                box-shadow:0 2px 10px rgba(123,13,30,.3);">Confirm</button>
            </div>
          </div>`;

        const style = document.createElement('style');
        style.textContent = `
          @keyframes fadeIn  { from { opacity:0; } to { opacity:1; } }
          @keyframes slideUp { from { opacity:0; transform:translateY(20px); } to { opacity:1; transform:none; } }
        `;
        document.head.appendChild(style);
        document.body.appendChild(overlay);

        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) closeModal();
        });
    }

    window.closeModal = function () {
        const m = document.getElementById('action-modal');
        if (m) m.style.display = 'none';
    };

    // Open modal for a status action
    window.openStatusAction = function (requirementId, action, docType, applicantEmail, applicantName) {
        buildModal();

        const modal     = document.getElementById('action-modal');
        const title     = document.getElementById('modal-title');
        const docInfo   = document.getElementById('modal-doc-info');
        const rejectDiv = document.getElementById('rejection-field');
        const remarksDiv= document.getElementById('remarks-field');
        const confirmBtn= document.getElementById('modal-confirm-btn');
        const select    = document.getElementById('rejection-reason-select');

        rejectDiv.style.display  = 'none';
        remarksDiv.style.display = 'none';

        const labels = {
            receive:    { title: 'Mark as Verified / Received', color: '#28a745' },
            reject:     { title: 'Reject Document',             color: '#dc3545' },
            resubmit:   { title: 'Flag for Resubmission',       color: '#c8a951' },
            review:     { title: 'Set to Under Review',         color: '#2e75b6' }
        };

        const cfg = labels[action] || labels.review;
        title.textContent = cfg.title;
        title.style.color = cfg.color;
        docInfo.textContent = docType + ' — ' + applicantName;

        confirmBtn.style.background = cfg.color;

        if (action === 'reject') {
            rejectDiv.style.display = 'block';
            loadRejectionReasons(select);
        } else if (action === 'resubmit') {
            remarksDiv.style.display = 'block';
        }

        confirmBtn.onclick = function () {
            executeAction(requirementId, action, applicantEmail, applicantName, docType);
        };

        modal.style.display = 'flex';
    };

    function loadRejectionReasons(select) {
        if (select.options.length > 1) return;
        fetch('/api/rejection-reasons/active')
            .then(function (r) { return r.json(); })
            .then(function (list) {
                list.forEach(function (r) {
                    const opt = document.createElement('option');
                    opt.value = r.reasonId;
                    opt.textContent = r.reasonName;
                    select.appendChild(opt);
                });
            })
            .catch(function () {});
    }

    function executeAction(requirementId, action, applicantEmail, applicantName, docType) {
        const confirmBtn = document.getElementById('modal-confirm-btn');
        confirmBtn.textContent = 'Processing…';
        confirmBtn.disabled = true;

        let url, body;

        if (action === 'receive') {
            url  = '/api/requirements/' + requirementId + '/receive';
            body = new URLSearchParams({
                processedBy:   '1',
                applicantEmail: applicantEmail,
                applicantName:  applicantName
            });
        } else if (action === 'reject') {
            const reasonId = document.getElementById('rejection-reason-select').value;
            if (!reasonId) {
                toast('Please select a rejection reason.', 'warning');
                confirmBtn.textContent = 'Confirm';
                confirmBtn.disabled = false;
                return;
            }
            url  = '/api/requirements/' + requirementId + '/reject';
            body = new URLSearchParams({
                rejectionReasonId: reasonId,
                processedBy:       '1',
                applicantEmail:    applicantEmail,
                applicantName:     applicantName
            });
        } else if (action === 'resubmit') {
            const remarks = document.getElementById('remarks-input').value.trim();
            url  = '/api/requirements/' + requirementId + '/resubmit';
            body = new URLSearchParams({
                remarks:    remarks,
                processedBy: '1'
            });
        }

        fetch(url, { method: 'POST', body: body })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                closeModal();
                if (data.message) {
                    toast(data.message, 'success');
                    setTimeout(function () { window.location.reload(); }, 1200);
                } else {
                    toast(data.error || 'Action failed.', 'error');
                }
            })
            .catch(function () {
                toast('Server error. Please try again.', 'error');
            })
            .finally(function () {
                confirmBtn.textContent = 'Confirm';
                confirmBtn.disabled = false;
            });
    }

    // ── Document upload mini-form ────────────────────────────────
    window.submitUpload = function (formId) {
        const form = document.getElementById(formId);
        if (!form) return;

        const data    = new FormData(form);
        const btn     = form.querySelector('[type=submit]');
        const origTxt = btn ? btn.textContent : '';

        if (btn) { btn.textContent = 'Uploading…'; btn.disabled = true; }

        fetch('/api/requirements/upload', { method: 'POST', body: data })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (res.trackingNo) {
                    toast('Document uploaded. Tracking no: ' + res.trackingNo, 'success');
                    form.reset();
                    setTimeout(function () { window.location.reload(); }, 1500);
                } else {
                    toast(res.error || 'Upload failed.', 'error');
                }
            })
            .catch(function () { toast('Upload failed. Check your connection.', 'error'); })
            .finally(function () {
                if (btn) { btn.textContent = origTxt; btn.disabled = false; }
            });
    };

    // ── Animate stat card counters ───────────────────────────────
    function animateCounters() {
        document.querySelectorAll('.card-count').forEach(function (el) {
            const target = parseInt(el.textContent, 10);
            if (isNaN(target) || target === 0) return;
            let current = 0;
            const step  = Math.ceil(target / 30);
            const timer = setInterval(function () {
                current = Math.min(current + step, target);
                el.textContent = current;
                if (current >= target) clearInterval(timer);
            }, 30);
        });
    }

    // ── Active nav item highlight ────────────────────────────────
    function highlightNav() {
        const path = window.location.pathname;
        document.querySelectorAll('.nav-item').forEach(function (link) {
            if (link.getAttribute('href') === path) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    // ── Confirm-before-logout ────────────────────────────────────
    function attachLogoutConfirm() {
        const logoutBtn = document.querySelector('.logout-btn');
        if (logoutBtn) {
            logoutBtn.removeAttribute('onclick');
            logoutBtn.addEventListener('click', function (e) {
                e.preventDefault();
                if (confirm('Sign out of PDTS?')) {
                    window.location.href = '/logout';
                }
            });
        }
    }

    // ── Search form: add spinner on submit ───────────────────────
    function attachSearchLoader() {
        const form = document.querySelector('.topbar-search');
        if (!form) return;
        form.addEventListener('submit', function () {
            const input = form.querySelector('.search-input');
            if (input) input.disabled = true;
        });
    }

    // ── Init ────────────────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', function () {
        renderDate();
        animateCounters();
        highlightNav();
        attachLogoutConfirm();
        attachSearchLoader();
    });

}());
