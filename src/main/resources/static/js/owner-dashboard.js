/* PawCare owner overview — exclusively uses existing authenticated application APIs. */
(() => {
  'use strict';
  const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  const count = n => Number.isFinite(Number(n)) ? Number(n).toLocaleString() : '—';
  const contents = page => Array.isArray(page?.content) ? page.content : [];
  const total = page => !page ? null : Number.isFinite(Number(page?.totalElements)) ? page.totalElements : contents(page).length;
  const dateLabel = value => {
    if (!value) return 'Date to be confirmed';
    const day = new Date(String(value).slice(0,10)+'T12:00:00');
    return Number.isNaN(day.getTime()) ? 'Date to be confirmed' : day.toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'});
  };
  const timeLabel = value => typeof value === 'string' && /^\d{2}:\d{2}/.test(value) ? value.slice(0,5) : '';
  const retry = (label, panel) => `<div class="owner-inline-error" role="alert"><i class="bi bi-wifi-off" aria-hidden="true"></i><span>${esc(label)}</span><button type="button" data-owner-retry="${panel}">Retry</button></div>`;
  const empty = (icon, title, desc, action, section) => `<div class="owner-empty-mini"><span class="owner-empty-mini-icon"><i class="bi ${icon}" aria-hidden="true"></i></span><strong>${esc(title)}</strong><p>${esc(desc)}</p>${action ? `<button type="button" data-owner-section="${section}" class="owner-inline-link">${esc(action)} <i class="bi bi-arrow-right" aria-hidden="true"></i></button>` : ''}</div>`;
  const byId = id => document.getElementById(id);
  const visitDate = b => {
    const raw = String(b.appointmentDate || '').slice(0,10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return Number.POSITIVE_INFINITY;
    const value = new Date(raw+'T'+(timeLabel(b.appointmentTime)||'00:00')+':00').getTime();
    return Number.isFinite(value) ? value : Number.POSITIVE_INFINITY;
  };
  const isUpcoming = visit => !['CANCELLED','COMPLETED','NO_SHOW','REJECTED','CANCELED'].includes(visit.status) && visitDate(visit) >= Date.now()-60000;
  let currentRun = 0;

  function setGreeting() {
    const user = typeof api !== 'undefined' ? api.user : null;
    const name = user?.firstName?.trim() || 'pet parent';
    byId('ownerName').textContent = name;
    byId('ownerToday').textContent = new Date().toLocaleDateString(undefined, {weekday:'long',month:'long',day:'numeric'});
    const h = new Date().getHours();
    byId('ownerWelcomeTitle').setAttribute('aria-label', `${h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'}, ${name}`);
    byId('ownerWelcomeTitle').firstChild.textContent = `${h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'}, `;
  }
  function renderStats(dashboard, appointments, grooming, orders) {
    const stats = [
      {name:'My pets',value:dashboard?.totalPets,icon:'bi-heart-fill',color:'violet',target:'pets',caption:'Your little family'},
      {name:'Vet appointments',value:total(appointments),icon:'bi-heart-pulse',color:'mint',target:'appointments',caption:'All your bookings'},
      {name:'Grooming visits',value:total(grooming),icon:'bi-scissors',color:'peach',target:'grooming',caption:'A little pampering'},
      {name:'Store orders',value:total(orders),icon:'bi-bag-heart',color:'rose',target:'orders',caption:'Your recent shopping'}
    ];
    byId('ownerStats').innerHTML = stats.map(item => `<button class="owner-stat owner-stat-${item.color}" type="button" data-owner-section="${item.target}" aria-label="${esc(item.name)}: ${item.value == null ? 'unavailable' : count(item.value)}; view ${esc(item.name)}"><span class="owner-stat-top"><span class="owner-stat-icon"><i class="bi ${item.icon}" aria-hidden="true"></i></span><i class="bi bi-arrow-up-right owner-stat-arrow" aria-hidden="true"></i></span><span class="owner-stat-value">${item.value == null?'—':count(item.value)}</span><span class="owner-stat-name">${esc(item.name)}</span><span class="owner-stat-caption">${esc(item.caption)}</span></button>`).join('');
    byId('ownerStats').setAttribute('aria-busy','false');
  }
  function renderUpcoming(appointments,grooming) {
    const destination = byId('ownerUpcoming');
    if(!appointments && !grooming) {destination.innerHTML=retry('Upcoming visits could not be loaded.','upcoming');return;}
    const visits = [
      ...contents(appointments).map(v=>({...v,kind:'Vet appointment',section:'appointments',icon:'bi-heart-pulse'})),
      ...contents(grooming).map(v=>({...v,kind:'Grooming visit',section:'grooming',icon:'bi-scissors'}))
    ].filter(isUpcoming).sort((a,b)=>visitDate(a)-visitDate(b)).slice(0,3);
    destination.innerHTML = visits.length ? `<div class="owner-visit-list">${visits.map(v=>`<button type="button" class="owner-visit" data-owner-section="${v.section}"><span class="owner-visit-icon"><i class="bi ${v.icon}" aria-hidden="true"></i></span><span class="owner-visit-main"><strong>${esc(v.petName||'Your pet')} <span class="owner-visit-kind">· ${esc(v.kind)}</span></strong><span>${esc(dateLabel(v.appointmentDate))}${v.appointmentTime?' · '+esc(timeLabel(v.appointmentTime)):''} ${v.veterinarianName?' · '+esc(v.veterinarianName):v.groomerName?' · '+esc(v.groomerName):''}</span></span><span class="owner-visit-chevron"><i class="bi bi-arrow-up-right" aria-hidden="true"></i></span></button>`).join('')}</div>` : empty('bi-calendar2-heart','A clear calendar, for now','When you book a vet or grooming visit, it will appear here.','Book an appointment','appointments');
  }
  function renderPets(pets, failed) {
    const destination = byId('ownerPetHighlights');
    if (failed) {destination.innerHTML=retry('Could not load your pets.','pets');return;}
    destination.innerHTML = pets?.length ? `<div class="owner-highlight-list">${pets.slice(0,3).map(p=>`<button type="button" class="owner-highlight-pet" data-owner-section="pets"><span class="owner-highlight-photo">${p.photo?`<img src="${esc(p.photo)}" alt="" loading="lazy" onerror="this.hidden=true">`:`<i class="bi bi-heart-fill" aria-hidden="true"></i>`}</span><span class="owner-highlight-text"><strong>${esc(p.name||'Pet')}</strong><small>${esc(p.breedName||p.speciesName||'My companion')}</small></span><i class="bi bi-chevron-right" aria-hidden="true"></i></button>`).join('')}</div>` : empty('bi-heart','No pet profiles yet','Create a pet profile to start their PawCare journey.','Add your first pet','addPet');
  }
  function renderActivity(page) {
    const destination = byId('ownerRecentActivity');
    if (!page) {destination.innerHTML=retry('Notifications could not be loaded.','activity');return;}
    const items=contents(page).slice(0,4);
    destination.innerHTML = items.length ? `<div class="owner-activity-list">${items.map(n=>{
      const href=typeof n.link==='string' && /^\/(?!\/)[a-zA-Z0-9/_#?=.&%-]*$/.test(n.link) ? n.link : '/pages/notifications.html';
      return `<a class="owner-activity" href="${esc(href)}"><span class="owner-activity-dot ${n.isRead?'':'owner-activity-unread'}"><i class="bi bi-bell" aria-hidden="true"></i></span><span><strong>${esc(n.title||'PawCare update')}</strong><small>${esc(n.message||'')}</small></span><i class="bi bi-arrow-up-right" aria-hidden="true"></i></a>`;
    }).join('')}</div>` : empty('bi-bell','All caught up!','New updates about your pets, visits and orders will appear here.');
  }
  async function load() {
    const run = ++currentRun;
    setGreeting();
    byId('ownerStats').setAttribute('aria-busy','true');
    const responses = await Promise.allSettled([
      api.getOwnerDashboard(), api.getMyAppointments(0), api.getMyGrooming(0), api.getMyOrders(0), api.getNotifications(0), loadPets()
    ]);
    if (run!==currentRun) return;
    const value = i => responses[i].status==='fulfilled' ? responses[i].value : null;
    renderStats(value(0),value(1),value(2),value(3));
    renderUpcoming(value(1),value(2));
    renderPets(typeof myPets !== 'undefined' ? myPets : [], responses[5].status==='rejected'||value(5)===false);
    renderActivity(value(4));
  }
  document.addEventListener('click', event => {
    const sectionLink=event.target.closest('[data-owner-section]');
    if(sectionLink && document.body.classList.contains('owner-dashboard-page')) {
      event.preventDefault();
      showSection(sectionLink.dataset.ownerSection);
      return;
    }
    const retryButton=event.target.closest('[data-owner-retry]');
    if(retryButton) {event.preventDefault(); load();}
  });
  window.PawCareOwnerOverview={load};
})();
