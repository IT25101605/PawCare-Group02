/* Shared site chrome: concise footer + same upper-right bell on every page. */
(() => {
  const footerMarkup = `<footer class="footer-section shared-site-footer" aria-label="PawCare information">
    <div class="shared-footer-inner">
      <div class="shared-footer-grid">
        <div class="shared-footer-about"><a class="shared-footer-brand" href="/"><img src="/images/paw.svg" alt="" width="28" height="28"><span>PawCare</span></a><p>A little care. A happier life.<br>Your companion in every chapter of pet parenting.</p></div>
        <div><h2>Our world</h2><a href="/pages/about.html">Meet PawCare</a><a href="/pages/services.html">Our services</a><a href="/pages/grooming.html">Grooming</a><a href="/pages/entertainment.html">Entertainment</a><a href="/pages/store.html">The pet store</a></div>
        <div><h2>Here for you</h2><a href="/pages/contact.html">Get in touch</a><a href="/pages/login.html">Your account</a><a href="/#how-it-works">How it works</a></div>
        <div><h2>Let's say hello</h2><a href="tel:+94715473916">+94 71 547 3916</a><a href="mailto:localit21@gmail.com">localit21@gmail.com</a><p>No. 46/1, Mihindu Mawatha,<br>Kurunegala, Sri Lanka</p></div>
      </div>
      <div class="shared-footer-bottom"><span>© 2026 PawCare. All paws welcome.</span><span>Made with care, for the ones you love.</span></div>
    </div>
  </footer>`;
  const notifyIcons = {APPOINTMENT:'bi-calendar-check',GROOMING:'bi-scissors',MEDICAL:'bi-heart-pulse',VACCINATION:'bi-shield-plus',PET_WEIGHT:'bi-speedometer2',ORDER:'bi-bag-check',DELIVERY:'bi-truck',INVENTORY:'bi-box-seam',ACCOUNT:'bi-shield-lock',ADMIN:'bi-person-check',PAYMENT:'bi-credit-card'};
  const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  function renderFooter(){
    const previous = document.querySelector('.footer-section');
    if (previous) previous.outerHTML = footerMarkup;
    else document.body.insertAdjacentHTML('beforeend', footerMarkup);
  }
  function renderNotifications(){
    const header = document.querySelector('#mainNavbar > .navbar-pill-container');
    if (!header || header.querySelector('.site-notify-button')) return;
    const loggedIn = typeof api !== 'undefined' && api.isLoggedIn();
    const bell = document.createElement(loggedIn ? 'button' : 'a');
    bell.className = 'site-notify-button';
    if(loggedIn) bell.type = 'button'; else {bell.href='/pages/login.html';bell.title='Sign in to see notifications';}
    bell.setAttribute('aria-label', loggedIn ? 'Notifications' : 'Sign in to view notifications');
    bell.innerHTML = '<i class="bi bi-bell" aria-hidden="true"></i><span class="site-notify-count" hidden>0</span>';
    header.append(bell);
    if(!loggedIn) return;
    const panel=document.createElement('aside');panel.className='site-notify-panel';panel.setAttribute('aria-label','Notification Center');panel.setAttribute('aria-hidden','true');
    panel.innerHTML=`<div class="site-panel-heading"><div><strong>Notifications</strong><small>Updates across PawCare</small></div><button class="site-notify-close" type="button" aria-label="Close notifications">×</button></div>
      <div class="site-notify-actions"><a href="/pages/notifications.html">View all</a><button type="button" data-site-read-all>Mark all as read</button><button type="button" data-site-refresh>Refresh</button></div>
      <div class="site-notify-items" aria-live="polite"><p>Loading notifications…</p></div>`;
    document.body.append(panel);
    const list=panel.querySelector('.site-notify-items'),counter=bell.querySelector('.site-notify-count');
    async function refreshCount(){try {const result=await api.getUnreadCount();const n=Number(result.count||0);counter.textContent=n>99?'99+':String(n);counter.hidden=n===0;}catch{counter.hidden=true;}}
    async function load(){
      list.innerHTML='<p>Loading notifications…</p>';
      try {
        const page=await api.getNotifications(0), entries=page.content||[];
        list.innerHTML=entries.length?entries.map(n=>`<article class="site-notify-item ${n.isRead?'':'is-unread'}" data-site-id="${Number(n.id)}">
          <i class="bi ${notifyIcons[n.notificationType]||'bi-bell'}" aria-hidden="true"></i><div><strong>${escapeHtml(n.title)}</strong><p>${escapeHtml(n.message)}</p><small>${n.createdAt?new Date(n.createdAt).toLocaleString():''}</small></div>
          <div class="site-notify-item-actions">${n.link?`<button type="button" data-site-open="${escapeHtml(n.link)}" aria-label="Open notification"><i class="bi bi-arrow-up-right"></i></button>`:''}<button type="button" data-site-delete="${Number(n.id)}" aria-label="Delete notification"><i class="bi bi-trash3"></i></button></div>
        </article>`).join(''):"<p>You are all caught up. No notifications yet.</p>";
        refreshCount();
      } catch {list.innerHTML='<p>Could not load notifications. Please try Refresh.</p>';}
    }
    function close(){panel.classList.remove('is-open');panel.setAttribute('aria-hidden','true');bell.setAttribute('aria-expanded','false');}
    bell.setAttribute('aria-expanded','false');bell.setAttribute('aria-controls','siteNotificationPanel');panel.id='siteNotificationPanel';
    bell.addEventListener('click',()=>{const open=!panel.classList.contains('is-open');if(!open){close();return;}panel.classList.add('is-open');panel.setAttribute('aria-hidden','false');bell.setAttribute('aria-expanded','true');load();});
    panel.querySelector('.site-notify-close').addEventListener('click',close);
    panel.querySelector('[data-site-refresh]').addEventListener('click',load);
    panel.querySelector('[data-site-read-all]').addEventListener('click',async()=>{try{await api.markAllNotificationsRead();await load();}catch{list.innerHTML='<p>Unable to update notifications.</p>';}});
    list.addEventListener('click',async e=>{
      const del=e.target.closest('[data-site-delete]');if(del){try{await api.deleteNotification(Number(del.dataset.siteDelete));await load();}catch{list.insertAdjacentHTML('afterbegin','<p>Delete failed.</p>');}return;}
      const open=e.target.closest('[data-site-open]');if(open){const item=open.closest('[data-site-id]');try{await api.markAsRead(Number(item.dataset.siteId));}catch{}const url=open.dataset.siteOpen;if(url.startsWith('/') && !url.startsWith('//')) location.href=url;return;}
      const item=e.target.closest('[data-site-id]');if(item?.classList.contains('is-unread')){try{await api.markAsRead(Number(item.dataset.siteId));item.classList.remove('is-unread');refreshCount();}catch{}}
    });
    document.addEventListener('click',e=>{if(!panel.contains(e.target)&&!bell.contains(e.target))close();});
    document.addEventListener('keydown',e=>{if(e.key==='Escape')close();});
    refreshCount();window.setInterval(()=>{if(!document.hidden)refreshCount();},60000);
  }
  function boot(){renderFooter();renderNotifications();}
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',boot,{once:true}); else boot();
})();
