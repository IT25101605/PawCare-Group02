/* The same site navigation on every page. Loaded immediately after its host. */
(() => {
  const nav = document.getElementById('mainNavbar');
  if (!nav) return;
  if (nav.dataset.sharedRendered) return;
  nav.dataset.sharedRendered = 'true';
  const pages = [['Home','/'],['About','/pages/about.html'],['Services','/pages/services.html'],['Grooming','/pages/grooming.html'],['Veterinarians','/pages/veterinarians.html'],['Store','/pages/store.html'],['Entertainment','/pages/entertainment.html'],['Contact','/pages/contact.html']];
  const current = location.pathname === '/index.html' ? '/' : location.pathname;
  nav.className = 'navbar navbar-pawcare shared-navbar';
  nav.setAttribute('aria-label', 'Main navigation');
  nav.innerHTML = `<div class="container navbar-pill-container">
    <a class="navbar-brand navbar-brand-pawcare" href="/" aria-label="PawCare homepage"><img class="brand-paw-icon" src="/images/paw.svg" alt="" width="32" height="32"><span class="brand-title">PawCare</span></a>
    <button class="navbar-toggler" type="button" aria-controls="navbarNav" aria-expanded="false" aria-label="Open navigation"><span aria-hidden="true">☰</span></button>
    <div class="navbar-collapse" id="navbarNav">
      <ul class="navbar-nav">${pages.map(([label,href]) => `<li class="nav-item"><a class="nav-link${current === href ? ' active' : ''}" href="${href}"${current === href ? ' aria-current="page"' : ''}>${label}</a></li>`).join('')}</ul>
      <div id="authButtons"><a class="nav-search-btn" href="/pages/store.html" aria-label="Search the pet store"><i class="bi bi-search" aria-hidden="true"></i></a><a class="btn btn-login-pill" href="/pages/login.html">Login</a><a class="btn btn-signup-pill" href="/pages/register.html">Sign Up</a></div>
      <a class="nav-dashboard-link" id="mainDashboardLink" href="/pages/dashboard/owner.html" style="display:none" aria-label="Open dashboard"><i class="bi bi-grid-1x2-fill" aria-hidden="true"></i> Dashboard</a>
      <div id="userMenu" class="dropdown" style="display:none"><button class="btn btn-signup-pill dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="bi bi-person-circle" aria-hidden="true"></i> <span class="user-name">My Account</span></button><ul class="dropdown-menu dropdown-menu-end"><li><a class="dropdown-item dash-link" href="/pages/dashboard/owner.html">Dashboard</a></li><li><a class="dropdown-item" href="/pages/notifications.html">Notifications</a></li><li><button class="dropdown-item" type="button" data-nav-logout>Logout</button></li></ul></div>
    </div></div>`;
  const dashboardCss=document.createElement('style');
  dashboardCss.textContent=`.nav-dashboard-link{display:inline-flex;align-items:center;justify-content:center;gap:7px;white-space:nowrap;text-decoration:none;font-weight:750;font-size:.87rem;color:#653480;background:#f2e8fb;border:1px solid #d9bbee;border-radius:999px;padding:9px 16px;box-shadow:0 2px 9px #59326615;transition:background .2s,transform .2s,box-shadow .2s}.nav-dashboard-link:hover,.nav-dashboard-link[aria-current=page]{color:#52256b;background:#e7d3f7;box-shadow:0 4px 16px #6a328530;transform:translateY(-1px)}@media(max-width:991.98px){.nav-dashboard-link{margin:9px 0;align-self:flex-start}}`;
  document.head.append(dashboardCss);
  // Paint the signed-in menu from the saved session before the account refresh completes.
  // The floating owner shortcut is not shown from cached roles alone.
  try {
    const cached=localStorage.getItem('pawcare_token') && JSON.parse(localStorage.getItem('pawcare_user') || 'null');
    if(cached) {
      nav.querySelector('#authButtons').style.setProperty('display','none','important');
      nav.querySelector('#userMenu').style.display='block';
      const dashboard = nav.querySelector('#mainDashboardLink');
      const destinations={PET_OWNER:'owner',VETERINARIAN:'vet',GROOMER:'groomer',ADMIN:'admin',STORE_MANAGER:'store',SERVICE_STAFF:'service'};
      dashboard.href='/pages/dashboard/'+(destinations[cached.role]||'owner')+'.html';
      dashboard.style.display='inline-flex';
      if(location.pathname===new URL(dashboard.href,location.origin).pathname)dashboard.setAttribute('aria-current','page');
      nav.querySelector('.user-name').textContent=[cached.firstName,cached.lastName].filter(Boolean).join(' ') || cached.email || 'My Account';
    }
  } catch (_) { /* The normal account refresh still handles missing session data. */ }
  const toggle = nav.querySelector('.navbar-toggler');
  const panel = nav.querySelector('#navbarNav');
  function setOpen(open) {
    panel.classList.toggle('show', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
  }
  toggle.addEventListener('click', () => setOpen(!panel.classList.contains('show')));
  window.matchMedia('(max-width:991.98px)').addEventListener('change', () => setOpen(false));
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && panel.classList.contains('show')) { setOpen(false); toggle.focus(); }
  });
  document.addEventListener('click', event => { if (!nav.contains(event.target)) setOpen(false); });
  nav.querySelectorAll('a').forEach(link => link.addEventListener('click', () => setOpen(false)));
  nav.querySelector('[data-nav-logout]').addEventListener('click', () => { if (typeof logout === 'function') logout(); });
  const userToggle = nav.querySelector('#userMenu > button');
  document.addEventListener('DOMContentLoaded', () => {
    // Keep the existing profile editor available on pages that load it.
    if (typeof openAccount !== 'function') return;
    const item = document.createElement('li');
    const button = document.createElement('button');
    button.type = 'button'; button.className = 'dropdown-item'; button.textContent = 'My Profile';
    button.addEventListener('click', () => openAccount());
    item.append(button); nav.querySelector('.dash-link').closest('li').after(item);
  });
  const closeUserMenu = () => {
    if (window.bootstrap) return;
    nav.querySelector('.dropdown-menu').classList.remove('show');
    userToggle.setAttribute('aria-expanded', 'false');
  };
  document.addEventListener('click', event => { if (!nav.querySelector('#userMenu').contains(event.target)) closeUserMenu(); });
  document.addEventListener('keydown', event => { if (event.key === 'Escape') closeUserMenu(); });
  userToggle.addEventListener('click', () => {
    if (window.bootstrap) return;
    const open = nav.querySelector('.dropdown-menu').classList.toggle('show');
    userToggle.setAttribute('aria-expanded', String(open));
  });
  // Dedicated owner entry point lives outside the account dropdown. Render only
  // after /api/auth/me has verified an authenticated PET_OWNER session.
  const ownerShortcut = document.createElement('a');
  ownerShortcut.className = 'pc-owner-shortcut';
  ownerShortcut.href = '/pages/dashboard/owner.html#overview';
  ownerShortcut.setAttribute('aria-label', 'My Dashboard — Pet Owner');
  ownerShortcut.setAttribute('title', 'My Dashboard');
  ownerShortcut.innerHTML = `<span class="pc-owner-shortcut-icon"><i class="bi bi-grid-1x2-fill" aria-hidden="true"></i><img src="/images/paw.svg" alt="" aria-hidden="true"></span><span class="pc-owner-shortcut-text">My Dashboard<small>Pet Owner</small></span><i class="bi bi-chevron-right pc-owner-shortcut-arrow" aria-hidden="true"></i>`;
  window.PawCareOwnerShortcut = {
    sync(verifiedOwner) {
      const ownerPage = location.pathname === '/pages/dashboard/owner.html';
      if (verifiedOwner && !ownerPage) {
        if (!ownerShortcut.isConnected) document.body.append(ownerShortcut);
      } else ownerShortcut.remove();
    }
  };
  const onScroll = () => nav.classList.toggle('scrolled', window.scrollY > 35);
  window.addEventListener('scroll', onScroll, { passive:true });
  onScroll();
})();
