/* Shared progressive enhancements. Business data stays with api.js and each page. */
(() => {
    const root = document.documentElement;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let paused = reducedMotion.matches;
    try { paused = paused || localStorage.getItem('pawcare-motion') === 'paused'; } catch (_) { /* Storage is optional. */ }
    function updateMotion() {
        root.classList.toggle('motion-paused', paused);
        document.querySelectorAll('.motion-toggle').forEach(button => {
            button.setAttribute('aria-pressed', String(paused));
            button.innerHTML = paused ? '<i class="bi bi-play-circle"></i> Resume motion' : '<i class="bi bi-pause-circle"></i> Pause motion';
        });
    }
    updateMotion();
    document.querySelectorAll('.motion-toggle').forEach(button => button.addEventListener('click', () => {
        paused = !paused;
        try { localStorage.setItem('pawcare-motion', paused ? 'paused' : 'playing'); } catch (_) { /* Storage is optional. */ }
        updateMotion();
    }));
    reducedMotion.addEventListener('change', event => { if (event.matches) paused = true; updateMotion(); });
    document.querySelectorAll('.reveal').forEach(element => element.classList.add('is-visible'));
    const roles = { ADMIN:'admin', PET_OWNER:'owner', VETERINARIAN:'vet', GROOMER:'groomer', STORE_MANAGER:'store', SERVICE_STAFF:'service' };
    document.querySelectorAll('[data-care-action]').forEach(link => {
        if (typeof api !== 'undefined' && api.isLoggedIn()) {
            const role = api.getRole();
            link.href = `/pages/dashboard/${roles[role] || 'owner'}.html${role === 'PET_OWNER' ? '#' + link.dataset.careAction : ''}`;
        }
    });
    // Attach existing visible labels to their controls without changing form submission.
    document.querySelectorAll('label:not([for])').forEach(label => {
        const field = label.parentElement.querySelector('input:not([type=hidden]),select,textarea');
        if (field) {
            if (!field.id) field.id = 'pawcare-field-' + Array.from(document.querySelectorAll('input,select,textarea')).indexOf(field);
            label.htmlFor = field.id;
        }
    });
    document.querySelectorAll('input[placeholder],textarea[placeholder]').forEach(field => {
        if (!field.labels?.length && !field.hasAttribute('aria-label')) field.setAttribute('aria-label', field.getAttribute('placeholder'));
    });
    document.querySelectorAll('.toggle-pw').forEach(button => {
        button.setAttribute('aria-label', 'Show password');
        button.addEventListener('click', () => button.setAttribute('aria-label', document.getElementById('password')?.type === 'text' ? 'Hide password' : 'Show password'));
    });
    const roleOptions = document.querySelectorAll('.role-option');
    roleOptions.forEach(option => {
        option.setAttribute('role', 'radio'); option.tabIndex = 0;
        option.setAttribute('aria-checked', String(option.classList.contains('selected')));
        option.addEventListener('keydown', event => {
            if (event.key === ' ' || event.key === 'Enter') { event.preventDefault(); option.click(); }
            if (['ArrowRight','ArrowDown','ArrowLeft','ArrowUp'].includes(event.key)) {
                event.preventDefault();
                const items = Array.from(roleOptions), direction = ['ArrowRight','ArrowDown'].includes(event.key) ? 1 : -1;
                const next = items[(items.indexOf(option) + direction + items.length) % items.length]; next.focus(); next.click();
            }
        });
        option.addEventListener('click', () => roleOptions.forEach(item => item.setAttribute('aria-checked', String(item.classList.contains('selected')))));
    });
    if (roleOptions.length) { roleOptions[0].closest('.row').setAttribute('role', 'radiogroup'); roleOptions[0].closest('.row').setAttribute('aria-label', 'Account type'); }
    const bookingLabels = { apptPet:'Your pet', apptVet:'Veterinarian', apptDate:'Appointment date', apptTime:'Time', apptReason:'Reason for visit', groomPet:'Your pet', groomGroomer:'Preferred groomer', groomDate:'Appointment date', groomTime:'Time' };
    Object.entries(bookingLabels).forEach(([id, title]) => {
        const field = document.getElementById(id);
        if (field && !field.labels.length) {
            const label = document.createElement('label'); label.className = 'form-label'; label.htmlFor = id; label.textContent = title;
            field.before(label); field.closest('.row')?.classList.add('align-items-end');
        }
    });
    const sidebar = document.querySelector('.dashboard-sidebar');
    if (sidebar) {
        document.body.classList.add('workspace-page');
        sidebar.setAttribute('aria-label', 'Workspace navigation');
        const close = document.createElement('button'); close.type = 'button'; close.className = 'sidebar-close'; close.setAttribute('aria-label', 'Close navigation'); close.innerHTML = '×'; sidebar.prepend(close);
        const backdrop = document.createElement('div'); backdrop.className = 'sidebar-backdrop'; document.body.append(backdrop);
        const trigger = document.querySelector('.dashboard-header .d-lg-none');
        trigger?.setAttribute('aria-label', 'Open workspace navigation'); trigger?.setAttribute('aria-controls', sidebar.id);
        const workspace = document.querySelector('.dashboard-content');
        const syncSidebar = () => {
            const mobile = window.innerWidth < 992;
            const open = mobile && sidebar.classList.contains('show');
            document.body.classList.toggle('workspace-nav-open', open);
            sidebar.inert = mobile && !open;
            if (workspace) workspace.inert = open;
            backdrop.classList.toggle('active', open);
            trigger?.setAttribute('aria-expanded', String(open));
            if (open && !sidebar.contains(document.activeElement)) close.focus();
        };
        const dismiss = () => { sidebar.classList.remove('show'); syncSidebar(); trigger?.focus(); };
        close.addEventListener('click', dismiss); backdrop.addEventListener('click', dismiss);
        new MutationObserver(syncSidebar).observe(sidebar, { attributes: true, attributeFilter: ['class'] });
        window.addEventListener('resize', syncSidebar, { passive: true });
        syncSidebar();
        document.addEventListener('keydown', event => { if (event.key === 'Escape' && sidebar.classList.contains('show')) dismiss(); });
        sidebar.querySelectorAll('.nav-link').forEach(link => link.addEventListener('click', () => {
            if (link.getAttribute('onclick')?.includes('showSection')) {
                sidebar.querySelectorAll('.nav-link').forEach(item => { item.classList.toggle('active', item === link); item.removeAttribute('aria-current'); });
                link.setAttribute('aria-current', 'page');
            }
            if (window.innerWidth < 992) sidebar.classList.remove('show');
        }));
        const header = document.querySelector('.dashboard-header>div');
        if (header) { const caption = document.createElement('div'); caption.className = 'workspace-caption'; caption.textContent = 'Your PawCare space'; header.prepend(caption); }
        const section = window.location.hash.slice(1);
        if (/^(pets|appointments|grooming|orders|addPet)$/.test(section)) {
            const target = Array.from(sidebar.querySelectorAll('.nav-link')).find(link => link.getAttribute('onclick')?.includes(`'${section}'`));
            target?.click();
        }
    }
    // Make icon-only controls discoverable for keyboard and assistive technology.
    document.querySelectorAll('button:not([aria-label])').forEach(button => {
        if (button.textContent.trim()) return;
        const icon = button.querySelector('i');
        const labels = {'bi-cart-plus':'Add to cart','bi-check':'Confirm booking','bi-eye':'Show password','bi-list':'Open navigation','bi-bell':'Notifications','bi-trash':'Delete','bi-pencil':'Edit'};
        if (icon) for (const [name, label] of Object.entries(labels)) if (icon.classList.contains(name)) { button.setAttribute('aria-label', label); break; }
    });
})();

// Escape transformed card containers so every Bootstrap dialog uses the same layer.
document.addEventListener('show.bs.modal', event => {
    const modal=event.target;
    if(modal.classList.contains('modal') && modal.parentElement!==document.body) document.body.append(modal);
});

// Start each workspace section at the top, including edits opened from a table.
if (document.querySelector('.dashboard-content') && typeof window.showSection === 'function') {
    const showWorkspaceSection=window.showSection;
    window.showSection=function(...args) {
        const result=showWorkspaceSection.apply(this,args);
        window.scrollTo(0,0);
        return result;
    };
}
