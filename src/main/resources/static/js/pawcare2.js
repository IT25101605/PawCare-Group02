/* PawCare 2.0 — presentation-only enhancement layer.
   This file intentionally does not alter API methods, routes, IDs, forms or business logic. */
(() => {
  'use strict';
  const path = location.pathname;
  if (path === '/' || path.endsWith('/index.html')) return;

  const body = document.body;
  body.classList.add('pc2-page');
  if (path.includes('/dashboard/')) body.classList.add('pc2-workspace');
  if (path.endsWith('/store.html')) body.classList.add('pc2-store');
  if (path.endsWith('/login.html')) body.classList.add('pc2-auth','pc2-login');
  if (path.endsWith('/register.html')) body.classList.add('pc2-register');

  const roleFromPath = () => {
    const p = path.split('/').pop()?.replace('.html','') || '';
    return ({owner:'Pet owner',admin:'Administrator',vet:'Veterinarian',groomer:'Grooming professional',store:'Store manager',service:'Service staff'})[p] || 'PawCare member';
  };

  function addAmbient(){
    if (document.querySelector('.pc2-ambient')) return;
    const ambient=document.createElement('div'); ambient.className='pc2-ambient'; ambient.setAttribute('aria-hidden','true'); body.prepend(ambient);
  }

  function enhanceWorkspace(){
    const content=document.querySelector('.dashboard-content');
    const header=document.querySelector('.dashboard-header');
    const sidebar=document.querySelector('.dashboard-sidebar');
    if(!content || !header || !sidebar) return;

    // Preserve the old navigation exactly; only add grouping labels visually.
    const nav=sidebar.querySelector('.sidebar-nav');
    if(nav && !nav.querySelector('.pc2-nav-label')){
      const items=[...nav.children];
      const first=document.createElement('div'); first.className='pc2-nav-label'; first.textContent='Workspace';
      first.style.cssText='padding:8px 12px 6px;color:rgba(255,255,255,.35);font-size:.57rem;font-weight:800;text-transform:uppercase;letter-spacing:.13em';
      nav.prepend(first);
      const hr=nav.querySelector('hr'); if(hr){const second=first.cloneNode();second.textContent='Explore';hr.after(second)}
    }

    if(!document.querySelector('.pc2-workspace-hero')){
      const hero=document.createElement('section'); hero.className='pc2-workspace-hero pc2-reveal';
      const name=document.querySelector('#ownerName,#adminName,#vetName,#groomerName,#storeName,#serviceName')?.textContent?.trim();
      const role=roleFromPath();
      const copy=role==='Pet owner' ? 'Everything for your pet’s health, appointments, grooming and everyday care — together in one calm workspace.' :
                 role==='Veterinarian' ? 'Review care, appointments and medical information through a focused clinical workspace.' :
                 role==='Store manager' ? 'Keep products, stock and customer orders clear, current and easy to act on.' :
                 role==='Grooming professional' ? 'Stay on top of grooming bookings and keep every pet visit beautifully organized.' :
                 role==='Administrator' ? 'A focused view of PawCare operations, people and platform activity.' :
                 'Your connected PawCare workspace for today’s tasks and customer care.';
      const artSrc=role==='Veterinarian'?'/images/pawcare2/medical-care.svg':role==='Store manager'?'/images/pawcare2/store-pet.svg':'/images/pawcare2/pawcare-orbit.svg?v=20261015-26';
      hero.innerHTML=`<div class="pc2-workspace-hero-copy"><span class="pc2-eyebrow"><i class="bi bi-stars"></i>${role}</span><h1>${name ? `Hello, ${escapeText(name)}.` : 'Care, beautifully connected.'}</h1><p>${copy}</p><div class="pc2-hero-chips"><span class="pc2-hero-chip"><i class="bi bi-shield-check"></i>Secure workspace</span><span class="pc2-hero-chip"><i class="bi bi-lightning-charge"></i>Quick actions</span><span class="pc2-hero-chip"><i class="bi bi-heart-pulse"></i>Live care data</span></div></div><div class="pc2-workspace-hero-art" aria-hidden="true"><img src="${artSrc}" alt=""></div>`;
      header.after(hero);
    }

    enhanceSections(content);
    observeSectionVisibility(content);
    decorateCards(content);
    improveAddPet();
    createMobileWorkspaceNav(sidebar);
  }

  function createMobileWorkspaceNav(sidebar){
    if(document.querySelector('.pc2-mobile-nav')) return;
    const source=[...sidebar.querySelectorAll('.sidebar-nav .nav-link')].filter(a=>!a.textContent.trim().toLowerCase().includes('logout') && !a.textContent.trim().toLowerCase().includes('back to site')).slice(0,4);
    if(!source.length) return;
    const nav=document.createElement('nav'); nav.className='pc2-mobile-nav'; nav.setAttribute('aria-label','Quick workspace navigation'); nav.style.setProperty('--pc2-mobile-count',String(source.length));
    source.forEach((a,index)=>{
      const c=a.cloneNode(true); c.removeAttribute('class'); c.className=a.classList.contains('active')?'active':'';
      const icon=c.querySelector('i')?.outerHTML||'<i class="bi bi-grid"></i>'; const label=(a.textContent||'Open').trim();
      c.innerHTML=`${icon}<span>${escapeText(label)}</span>`;
      c.addEventListener('click',()=>nav.querySelectorAll('a').forEach(x=>x.classList.toggle('active',x===c)));
      nav.append(c);
    });
    body.append(nav);
  }

  function enhanceSections(content){
    const descriptions={
      pets:['Your pets','Profiles, health history and everyday care at a glance.'],
      appointments:['Appointments','Plan visits and keep upcoming care easy to follow.'],
      grooming:['Grooming','Manage fresh trims and grooming visits in one place.'],
      orders:['Orders','Track purchases, payments and order progress.'],
      addPet:['Create a pet profile','Add the details that make care more personal.'],
      users:['People & access','Manage PawCare accounts and stakeholder access.'],
      products:['Products','Keep the PawCare store curated, useful and in stock.'],
      medical:['Medical records','A clearer view of pet health history and treatment.'],
      vaccinations:['Vaccinations','Track preventive care and vaccination information.'],
      dashboard:['Overview','The most important PawCare activity, together.']
    };
    content.querySelectorAll(':scope > [id^="section-"]').forEach((section,index)=>{
      section.classList.add('pc2-section-shell'); section.style.setProperty('--pc2-delay',`${Math.min(index*45,180)}ms`);
      if(section.querySelector(':scope > .pc2-section-heading')) return;
      const key=section.id.replace('section-','');
      const candidate=section.querySelector(':scope > h5, :scope > .d-flex > h5, :scope > h4, :scope > .d-flex > h4');
      if(!candidate) return;
      const tuple=descriptions[key]||[candidate.textContent.trim(),'Focused tools and information for this part of PawCare.'];
      const oldParent=candidate.parentElement;
      const heading=document.createElement('div'); heading.className='pc2-section-heading';
      const copy=document.createElement('div'); copy.className='pc2-copy';
      const kicker=document.createElement('span'); kicker.className='pc2-section-kicker'; kicker.textContent='PawCare 2.0';
      const description=document.createElement('p'); description.textContent=tuple[1];
      heading.append(copy);

      // Keep the original heading node (and its id/listeners) alive. Several dashboard
      // functions update these elements dynamically, so replacing them breaks CRUD flows.
      if(oldParent?.classList.contains('d-flex')){
        oldParent.before(heading);
        copy.append(kicker,candidate,description);
        [...oldParent.children].forEach(el=>heading.append(el));
        oldParent.remove();
      } else {
        candidate.before(heading);
        copy.append(kicker,candidate,description);
      }
      // Match the PawCare 2.0 wording for static headings, but never overwrite dynamic editor titles.
      if(!candidate.id) candidate.textContent=tuple[0];
    });
  }

  function observeSectionVisibility(content){
    const sections=[...content.querySelectorAll(':scope > [id^="section-"]')];
    // Track only hidden/visible state. Never rewrite the observed style attribute:
    // doing so can recursively retrigger MutationObserver and lock the page.
    const visibleState=new WeakMap();
    sections.forEach(section=>visibleState.set(section,getComputedStyle(section).display!=='none'));
    const observer=new MutationObserver(mutations=>{
      for(const mutation of mutations){
        const section=mutation.target;
        if(!(section instanceof HTMLElement) || !section.id?.startsWith('section-')) continue;
        const visible=getComputedStyle(section).display!=='none';
        const wasVisible=visibleState.get(section);
        visibleState.set(section,visible);
        if(!visible || wasVisible===visible) continue;
        section.classList.remove('pc2-section-enter');
        // Class changes are not observed here, so this is a one-shot animation with no feedback loop.
        requestAnimationFrame(()=>{
          section.classList.add('pc2-section-enter');
          window.setTimeout(()=>section.classList.remove('pc2-section-enter'),450);
        });
      }
    });
    sections.forEach(section=>observer.observe(section,{attributes:true,attributeFilter:['style']}));
  }

  function decorateCards(root){
    [...root.querySelectorAll('.card-pawcare,.stat-card')].forEach((card,i)=>{card.classList.add('pc2-reveal');card.style.setProperty('--pc2-delay',`${Math.min(i*32,210)}ms`)});
    root.querySelectorAll('img[id$="Preview"]').forEach(img=>img.classList.add('photo-preview-polished'));
  }

  function improveAddPet(){
    const section=document.getElementById('section-addPet'); if(!section) return;
    const photo=document.getElementById('petPhoto'); if(photo && !photo.dataset.pc2){
      photo.dataset.pc2='true';
      const hint=photo.parentElement?.querySelector('small'); if(hint) hint.innerHTML='<i class="bi bi-image me-1"></i>JPEG or PNG • up to 5 MB • optional';
    }
    const dob=document.getElementById('petDob'); if(dob) dob.max=new Date().toISOString().split('T')[0];
  }

  function enhanceAuth(){
    const left=document.querySelector('.auth-left'); if(left && !left.querySelector('.pc2-auth-video')){
      const video=document.createElement('video');video.className='pc2-auth-video';video.autoplay=true;video.muted=true;video.loop=true;video.playsInline=true;video.setAttribute('aria-hidden','true');
      video.innerHTML='<source src="/media/pawcare-ambient.mp4" type="video/mp4">';left.prepend(video);
    }
    document.querySelector('.auth-form-card')?.classList.add('pc2-reveal');
  }

  function enhanceRegister(){
    document.querySelector('.reg-card')?.classList.add('pc2-reveal');
    const page=document.querySelector('.reg-page');
    if(page && !page.querySelector('.pc2-register-visual')){
      const visual=document.createElement('img'); visual.className='pc2-register-visual pc2-float'; visual.src='/images/pawcare2/pawcare-orbit.svg?v=20261015-26'; visual.alt=''; visual.setAttribute('aria-hidden','true'); visual.style.cssText='position:fixed;right:-120px;top:7%;width:min(520px,38vw);opacity:.14;pointer-events:none;z-index:-1'; page.prepend(visual);
    }
  }

  function enhancePublic(){
    document.querySelectorAll('.section-padding .card-pawcare,.section-padding .service-card,.section-padding .feature-card,.product-card').forEach((el,i)=>{el.classList.add('pc2-reveal');el.style.setProperty('--pc2-delay',`${Math.min(i*45,270)}ms`)});
    const mast=document.querySelector('.page-masthead'); if(mast) mast.classList.add('pc2-reveal');
    if(body.classList.contains('pc2-store')){
      const masthead=document.querySelector('.page-masthead');
      if(masthead && !masthead.querySelector('.pc2-store-art')){
        const art=document.createElement('img');art.className='pc2-store-art pc2-float';art.src='/images/pawcare2/store-pet.svg';art.alt='';art.setAttribute('aria-hidden','true');art.style.cssText='position:absolute;right:5%;bottom:-28px;width:min(430px,37vw);opacity:.34;pointer-events:none';masthead.append(art);
      }
    }
  }


  function notificationIcon(type){
    const map={APPOINTMENT:'bi-calendar-check',GROOMING:'bi-scissors',MEDICAL:'bi-heart-pulse',VACCINATION:'bi-shield-plus',PET_WEIGHT:'bi-speedometer2',ORDER:'bi-bag-check',DELIVERY:'bi-truck',INVENTORY:'bi-box-seam',ACCOUNT:'bi-shield-lock',ADMIN:'bi-person-check',PAYMENT:'bi-credit-card'};
    return map[type]||'bi-bell';
  }

  function initNotificationCenter(){
    if(typeof api==='undefined'||!api.isLoggedIn()||document.querySelector('.pc2-notify-button'))return;
    const host=document.querySelector('.dashboard-header')||document.body;
    const button=document.createElement('button');button.type='button';button.className='pc2-notify-button';button.setAttribute('aria-label','Notifications');
    button.innerHTML='<i class="bi bi-bell"></i><span class="pc2-notify-count" hidden>0</span>';
    if(host!==document.body)host.append(button);else document.body.append(button);
    const panel=document.createElement('aside');panel.className='pc2-notify-panel';panel.setAttribute('aria-label','Notification Center');panel.innerHTML=`
      <div class="pc2-panel-head"><div><strong>Notifications</strong><small>Updates across PawCare</small></div><button class="pc2-panel-close" aria-label="Close">×</button></div>
      <div class="pc2-notify-actions"><a href="/pages/notifications.html">View all</a><button data-action="read-all">Mark all as read</button><button data-action="refresh">Refresh</button></div>
      <div class="pc2-notify-list"><div class="pc2-panel-loading">Loading notifications…</div></div>`;
    document.body.append(panel);
    const list=panel.querySelector('.pc2-notify-list'),count=button.querySelector('.pc2-notify-count');
    async function refreshCount(){try{const data=await api.getUnreadCount();const n=Number(data.count||0);count.textContent=n>99?'99+':n;count.hidden=!n;}catch{}}
    async function load(){
      list.innerHTML='<div class="pc2-panel-loading">Loading notifications…</div>';
      try{
        const page=await api.getNotifications(0),items=page.content||[];
        list.innerHTML=items.length?items.map(n=>`<article class="pc2-notify-item ${n.isRead?'':'is-unread'}" data-id="${n.id}">
          <div class="pc2-notify-icon"><i class="bi ${notificationIcon(n.notificationType)}"></i></div>
          <div class="pc2-notify-copy"><strong>${escapeText(n.title)}</strong><p>${escapeText(n.message)}</p><small>${new Date(n.createdAt).toLocaleString()}</small></div>
          <div class="pc2-notify-item-actions">${n.link?`<button data-open="${escapeText(n.link)}" title="Open"><i class="bi bi-arrow-up-right"></i></button>`:''}<button data-delete="${n.id}" title="Delete"><i class="bi bi-trash3"></i></button></div>
        </article>`).join(''):'<div class="pc2-panel-empty"><i class="bi bi-bell-slash"></i><strong>You’re all caught up</strong><span>No notifications yet.</span></div>';
        await refreshCount();
      }catch(e){list.innerHTML=`<div class="pc2-panel-empty"><i class="bi bi-exclamation-triangle"></i><strong>Unable to load notifications</strong><span>${escapeText(e.message||'Please try again.')}</span></div>`}
    }
    button.addEventListener('click',()=>{panel.classList.toggle('is-open');if(panel.classList.contains('is-open'))load();});
    panel.querySelector('.pc2-panel-close').addEventListener('click',()=>panel.classList.remove('is-open'));
    panel.querySelector('[data-action="refresh"]').addEventListener('click',load);
    panel.querySelector('[data-action="read-all"]').addEventListener('click',async()=>{try{await api.markAllNotificationsRead();await load();}catch(e){showToast(e.message,'error')}});
    list.addEventListener('click',async e=>{
      const del=e.target.closest('[data-delete]');if(del){try{await api.deleteNotification(Number(del.dataset.delete));await load();}catch(err){showToast(err.message,'error')}return;}
      const open=e.target.closest('[data-open]');if(open){const item=open.closest('[data-id]');try{await api.markAsRead(Number(item.dataset.id));}catch{} location.href=open.dataset.open;return;}
      const item=e.target.closest('[data-id]');if(item&&item.classList.contains('is-unread')){try{await api.markAsRead(Number(item.dataset.id));item.classList.remove('is-unread');await refreshCount();}catch{}}
    });
    refreshCount();window.setInterval(refreshCount,60000);
  }

  function initAssistant(){
    if(typeof api==='undefined'||!api.isLoggedIn()||document.querySelector('.pc2-assistant-button'))return;
    const button=document.createElement('button');button.type='button';button.className='pc2-assistant-button';button.setAttribute('aria-label','Open PawCare Assistant');button.innerHTML='<i class="bi bi-stars"></i><span>PawCare Assistant</span>';
    const panel=document.createElement('aside');panel.className='pc2-assistant-panel';panel.innerHTML=`
      <div class="pc2-panel-head"><div><strong><i class="bi bi-stars me-2"></i>PawCare Assistant</strong><small>General guidance & PawCare help</small></div><button class="pc2-panel-close" aria-label="Close">×</button></div>
      <div class="pc2-assistant-messages"><div class="pc2-assistant-msg bot">Hi! Ask me about PawCare, pet-care terms, appointments, grooming, vaccinations or shipping.</div></div>
      <div class="pc2-assistant-suggestions"><button>How do I book a veterinarian?</button><button>How is shipping calculated?</button><button>What is a booster vaccination?</button></div>
      <form class="pc2-assistant-form"><input maxlength="1000" placeholder="Ask PawCare…" aria-label="Ask PawCare Assistant"><button aria-label="Send"><i class="bi bi-send"></i></button></form>
      <small class="pc2-assistant-notice">General information only. The assistant does not diagnose pets.</small>`;
    document.body.append(button,panel);
    const messages=panel.querySelector('.pc2-assistant-messages'),form=panel.querySelector('form'),input=form.querySelector('input');
    button.addEventListener('click',()=>{panel.classList.toggle('is-open');if(panel.classList.contains('is-open'))input.focus();});
    panel.querySelector('.pc2-panel-close').addEventListener('click',()=>panel.classList.remove('is-open'));
    async function ask(text){
      const q=String(text||'').trim();if(!q)return;
      messages.insertAdjacentHTML('beforeend',`<div class="pc2-assistant-msg user">${escapeText(q)}</div><div class="pc2-assistant-msg bot is-typing">Thinking…</div>`);messages.scrollTop=messages.scrollHeight;input.value='';
      const typing=messages.querySelector('.is-typing');
      try{const reply=await api.askAssistant(q);typing.classList.remove('is-typing');typing.textContent=reply.answer;}
      catch(e){typing.classList.remove('is-typing');typing.textContent=e.message||'PawCare Assistant is temporarily unavailable.';}
      messages.scrollTop=messages.scrollHeight;
    }
    form.addEventListener('submit',e=>{e.preventDefault();ask(input.value)});
    panel.querySelectorAll('.pc2-assistant-suggestions button').forEach(b=>b.addEventListener('click',()=>ask(b.textContent)));
  }

  function initAvailabilityManager(){
    if(typeof api==='undefined'||!api.isLoggedIn()||!['VETERINARIAN','GROOMER'].includes(api.getRole())||document.querySelector('.pc2-availability-button'))return;
    const header=document.querySelector('.dashboard-header');if(!header)return;
    const button=document.createElement('button');button.type='button';button.className='btn btn-pawcare-outline pc2-availability-button';button.innerHTML='<i class="bi bi-clock-history me-1"></i>Availability';header.append(button);
    const panel=document.createElement('aside');panel.className='pc2-availability-panel';panel.innerHTML=`<div class="pc2-panel-head"><div><strong>Weekly availability</strong><small>Owners only see open time slots.</small></div><button class="pc2-panel-close" aria-label="Close">×</button></div><div class="pc2-availability-list"></div><div class="pc2-block-date"><strong>Block a date / leave</strong><div><input type="date" data-block-date><input maxlength="200" data-block-reason placeholder="Reason (optional)"><button data-block-save>Block date</button></div><div data-blocked-dates></div></div>`;document.body.append(panel);
    const list=panel.querySelector('.pc2-availability-list'),blocked=panel.querySelector('[data-blocked-dates]');
    async function loadBlocked(){
      try{const dates=await api.getMyUnavailableDates();blocked.innerHTML=dates.length?dates.map(d=>`<div class="pc2-blocked-date"><span>${escapeText(d.date)}${d.reason?' · '+escapeText(d.reason):''}</span><button data-unblock="${d.id}">Remove</button></div>`).join(''):'<small class="text-muted">No blocked dates.</small>';}catch(e){blocked.innerHTML='<small class="text-danger">'+escapeText(e.message)+'</small>'}
    }
    async function load(){
      list.innerHTML='<div class="pc2-panel-loading">Loading availability…</div>';
      try{const rows=await api.getProviderAvailability(api.user.id);list.innerHTML=rows.map(a=>`<div class="pc2-availability-row" data-day="${a.dayOfWeek}"><label><input type="checkbox" ${a.active?'checked':''}> ${a.dayOfWeek.charAt(0)+a.dayOfWeek.slice(1).toLowerCase()}</label><input type="time" data-start value="${a.startTime}"><span>to</span><input type="time" data-end value="${a.endTime}"><button>Save</button></div>`).join('');await loadBlocked();}
      catch(e){list.innerHTML=`<div class="pc2-panel-empty"><strong>Unable to load availability</strong><span>${escapeText(e.message)}</span></div>`}
    }
    button.addEventListener('click',()=>{panel.classList.toggle('is-open');if(panel.classList.contains('is-open'))load();});
    panel.querySelector('.pc2-panel-close').addEventListener('click',()=>panel.classList.remove('is-open'));
    list.addEventListener('click',async e=>{const save=e.target.closest('button');if(!save)return;const row=save.closest('[data-day]');save.disabled=true;try{await api.saveMyAvailability({dayOfWeek:row.dataset.day,startTime:row.querySelector('[data-start]').value,endTime:row.querySelector('[data-end]').value,active:row.querySelector('input[type=checkbox]').checked});showToast('Availability saved.','success');}catch(err){showToast(err.message,'error')}finally{save.disabled=false;}});
    panel.querySelector('[data-block-save]').addEventListener('click',async()=>{const date=panel.querySelector('[data-block-date]').value,reason=panel.querySelector('[data-block-reason]').value;if(!date){showToast('Choose a date to block.','warning');return;}try{await api.addMyUnavailableDate({date,reason});panel.querySelector('[data-block-date]').value='';panel.querySelector('[data-block-reason]').value='';await loadBlocked();showToast('Date blocked.','success');}catch(e){showToast(e.message,'error')}});
    blocked.addEventListener('click',async e=>{const b=e.target.closest('[data-unblock]');if(!b)return;try{await api.deleteMyUnavailableDate(Number(b.dataset.unblock));await loadBlocked();showToast('Blocked date removed.','success');}catch(err){showToast(err.message,'error')}});
  }

  function escapeText(value){return String(value??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]))}

  function boot(){
    if(body.dataset.pc2Booted) return;
    body.dataset.pc2Booted='true';
    addAmbient();
    if(body.classList.contains('workspace-page')) enhanceWorkspace();
    if(body.classList.contains('pc2-auth')) enhanceAuth();
    else if(body.classList.contains('pc2-register')) enhanceRegister();
    else enhancePublic();
    // The shared top-nav notification center is initialized by site-shell.js.
    initAvailabilityManager();
    body.classList.add('pc2-page-ready');
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',boot,{once:true}); else boot();
})();
