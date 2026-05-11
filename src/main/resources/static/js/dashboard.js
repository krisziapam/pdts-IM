document.addEventListener('DOMContentLoaded', () => {
    const search = document.querySelector('[data-table-search]');
    const type = document.querySelector('[data-table-type]');
    const table = document.querySelector('[data-filterable-table]');
    const applyTableFilter = () => {
        if (!table) return;
        const q = (search?.value || '').toLowerCase().trim();
        const docType = (type?.value || '').toLowerCase().trim();
        table.querySelectorAll('tbody tr').forEach(row => {
            const text = row.innerText.toLowerCase();
            const matchesSearch = !q || text.includes(q);
            const matchesType = !docType || text.includes(docType);
            row.style.display = matchesSearch && matchesType ? '' : 'none';
        });
    };
    search?.addEventListener('input', applyTableFilter);
    type?.addEventListener('change', applyTableFilter);

    document.querySelectorAll('.role-card input[type="radio"]').forEach(input => {
        input.addEventListener('change', () => {
            document.querySelectorAll('.role-card').forEach(card => card.classList.remove('selected'));
            input.closest('.role-card')?.classList.add('selected');
        });
    });
});
