(() => {
  const services = {
    veterinary: {title:'Veterinary care', image:'/images/vet.png', alt:'A welcoming veterinary care space with pets and a veterinarian', description:'From routine checkups to ongoing support, find the right care and keep their health records connected.', href:'/pages/veterinarians.html', link:'Find a veterinarian'},
    grooming: {title:'Grooming & spa', image:'/images/grooming.png', alt:'A happy dog in a lavender grooming studio', description:'Gentle baths, fresh trims and thoughtful finishing touches to help your companion look and feel their best.', href:'/pages/grooming.html', link:'Explore grooming'},
    store: {title:'Pet essentials', image:'/images/petstore.png', alt:'Pet food, toys and accessories in a lilac pet boutique', description:'Food, toys and everyday comforts. Find the essentials that make mealtimes, playtimes and life together better.', href:'/pages/store.html', link:'Explore the pet store'}
  };
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const tabs = [...document.querySelectorAll('[data-service]')];
  const card = document.getElementById('service-preview');
  function zoomCard() {
    if (reducedMotion.matches) return;
    card.classList.remove('is-zooming');
    void card.offsetWidth;
    card.classList.add('is-zooming');
  }
  let activeService='veterinary';
  let transitionTimer=0;
  let transitionToken=0;
  function renderService(key) {
    const service=services[key];
    card.setAttribute('aria-labelledby','tab-'+key);
    document.getElementById('preview-title').textContent=service.title;
    const image=document.getElementById('preview-image');
    image.src=service.image;image.alt=service.alt;
    document.getElementById('preview-description').textContent=service.description;
    const link=document.getElementById('preview-link'); link.href=service.href;
    const arrow=document.createElement('span');arrow.textContent='↗';arrow.setAttribute('aria-hidden','true');
    link.replaceChildren(service.link,arrow);
  }
  function selectService(key) {
    if (!services[key] || key===activeService) return;
    activeService=key;
    tabs.forEach(tab=>{const selected=tab.dataset.service===key;tab.setAttribute('aria-selected',String(selected));tab.tabIndex=selected?0:-1;});
    if (transitionTimer) clearTimeout(transitionTimer);
    transitionToken++;
    const token=transitionToken;
    card.classList.remove('is-zooming','is-changing','is-flipping','is-flip-edge','is-flipping-in');
    if (reducedMotion.matches) {renderService(key);return;}
    // Update hidden content at the card's 90-degree (edge-on) midpoint.
    // An out/in 3D flip avoids mirrored words on the reverse face.
    card.classList.add('is-flipping');
    transitionTimer=setTimeout(()=>{
      if (token!==transitionToken) return;
      renderService(activeService);
      card.classList.remove('is-flipping');
      card.classList.add('is-flip-edge');
      // Same edge silhouette at +90 degrees, then complete the turn to the front.
      void card.offsetWidth;
      requestAnimationFrame(()=>{
        if(token!==transitionToken)return;
        card.classList.remove('is-flip-edge');
        card.classList.add('is-flipping-in');
        transitionTimer=setTimeout(()=>{card.classList.remove('is-flipping-in');transitionTimer=0;},300);
      });
    },260);
  }
  tabs.forEach((tab,index) => {
    tab.addEventListener('click', () => selectService(tab.dataset.service));
    tab.addEventListener('keydown', event => {
      let next;
      if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
      if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
      if (event.key === 'Home') next = 0;
      if (event.key === 'End') next = tabs.length - 1;
      if (next === undefined) return;
      event.preventDefault(); tabs[next].focus(); selectService(tabs[next].dataset.service);
    });
  });
  document.getElementById('preview-zoom').addEventListener('click', zoomCard);
  card.addEventListener('click', event => { if (!event.target.closest('a,button')) zoomCard(); });
  card.addEventListener('keydown', event => {
    if (event.target === card && ['Enter',' '].includes(event.key)) { event.preventDefault(); zoomCard(); }
  });
  card.addEventListener('animationend', () => card.classList.remove('is-zooming','is-changing'));

})();
