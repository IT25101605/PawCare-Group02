/* One continuous loading screen for full-page navigation. */
(() => {
    if (window.pawcarePageLoading) return;
    const minimumVisible=280, fadeDuration=140, navigationKey='pawcare-loading-navigation';
    let loader, dismissTimer, recoveryTimer, startedAt=Date.now();
    try {
        const navigation=JSON.parse(sessionStorage.getItem(navigationKey)||'null');
        sessionStorage.removeItem(navigationKey);
        if(navigation?.target===location.origin+location.pathname+location.search && Date.now()-navigation.time<10000)startedAt=navigation.time;
    }catch{}
    function show(navigationURL) {
        document.documentElement.classList.add('paw-is-loading');
        clearTimeout(dismissTimer); clearTimeout(recoveryTimer);
        if(navigationURL) {
            startedAt=Date.now();
            try{sessionStorage.setItem(navigationKey,JSON.stringify({target:navigationURL.origin+navigationURL.pathname+navigationURL.search,time:startedAt}));}catch{}
        }
        if (!loader || !loader.isConnected) {
            loader=document.createElement('div'); loader.className='paw-page-loader';
            loader.setAttribute('role','status'); loader.setAttribute('aria-live','polite');
            loader.innerHTML='<div class="paw-loading-card"><div class="paw-loading-mark" aria-hidden="true"><img src="/images/paw.svg" alt="" width="36" height="36"></div><strong>PawCare</strong><span>A little care. Almost there.</span><div class="paw-loading-progress" aria-hidden="true"><i></i></div></div>';
            loader.style.setProperty('--paw-loading-delay',-(Date.now()-startedAt)+'ms');
            document.body.append(loader);
        }
        loader.classList.remove('is-leaving');
        document.body.setAttribute('aria-busy','true');
        window.dispatchEvent(new Event('pawcare:loading'));
        recoveryTimer=setTimeout(()=>hide(true),8000);
    }
    function hide(immediate=false) {
        clearTimeout(recoveryTimer); clearTimeout(dismissTimer);
        if (!loader) return;
        if (immediate === true) {
            document.body.removeAttribute('aria-busy');loader.remove();loader=null;
            document.documentElement.classList.remove('paw-is-loading');
            try{sessionStorage.removeItem(navigationKey);}catch{}
            window.dispatchEvent(new Event('pawcare:page-ready'));
            return;
        }
        dismissTimer=setTimeout(()=>{
            document.body.removeAttribute('aria-busy');
            loader?.classList.add('is-leaving');
            dismissTimer=setTimeout(()=>{loader?.remove();loader=null;document.documentElement.classList.remove('paw-is-loading');window.dispatchEvent(new Event('pawcare:page-ready'));},fadeDuration);
        },Math.max(0,minimumVisible-(Date.now()-startedAt)));
    }
    window.pawcarePageLoading={show,hide};
    show();
    const ready=()=>{if(document.fonts?.ready)document.fonts.ready.then(()=>hide());else hide();};
    if (document.readyState==='loading') document.addEventListener('DOMContentLoaded',ready,{once:true});
    else ready();
    window.addEventListener('pageshow',event=>{if(event.persisted)hide(true);});
    document.addEventListener('click',event=>{
        if(event.defaultPrevented || event.button!==0 || event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
        const link=event.target.closest?.('a[href]');
        if(!link || link.hasAttribute('download') || (link.target && link.target!=='_self')) return;
        let url;try{url=new URL(link.href,location.href);}catch{return;}
        if(url.origin!==location.origin || !['http:','https:'].includes(url.protocol)) return;
        if(url.pathname===location.pathname && url.search===location.search) return;
        // Other click handlers may open a dialog or handle navigation themselves.
        // A new task runs after all handlers; a microtask can run between listeners.
        setTimeout(()=>{if(!event.defaultPrevented)show(url);},0);
    });
})();
