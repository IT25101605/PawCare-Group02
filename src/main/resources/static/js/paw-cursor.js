/* Decorative only: never capture clicks, focus, text selection or scrolling. */
(()=>{
    if(document.querySelector('.paw-cursor-layer'))return;
    const mouse=matchMedia('(hover:hover) and (pointer:fine)'),contrast=matchMedia('(forced-colors:active)');
    if(!mouse.matches||contrast.matches)return;
    const root=document.documentElement,layer=document.createElement('div');layer.className='paw-cursor-layer';layer.setAttribute('aria-hidden','true');
    layer.innerHTML=`<div class="paw-cursor"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 44 50" fill="none">
      <defs><linearGradient id="paw-fur" x1="10" y1="18" x2="35" y2="48" gradientUnits="userSpaceOnUse"><stop stop-color="#fffdfd"/><stop offset="1" stop-color="#e9ddf4"/></linearGradient><linearGradient id="paw-beans" x1="14" y1="22" x2="30" y2="39" gradientUnits="userSpaceOnUse"><stop stop-color="#efb8d2"/><stop offset="1" stop-color="#ba80bf"/></linearGradient></defs>
      <path d="M11 48 9 39c-4-4-4-11-1-16 3-6 24-6 28 0 3 5 3 12-1 16l-2 9-5-2-6 3-6-3-5 2Z" fill="url(#paw-fur)" stroke="#805e9a" stroke-width="1.2" stroke-linejoin="round"/>
      <g class="paw-toe paw-toe-left"><ellipse cx="8.5" cy="19" rx="6.2" ry="9" transform="rotate(-25 8.5 19)" fill="url(#paw-fur)" stroke="#805e9a" stroke-width="1.2"/><ellipse cx="9" cy="19.5" rx="3.4" ry="4.8" transform="rotate(-25 9 19.5)" fill="url(#paw-beans)"/></g>
      <g class="paw-toe paw-toe-middle-left"><ellipse cx="17" cy="12" rx="6" ry="9.5" transform="rotate(-7 17 12)" fill="url(#paw-fur)" stroke="#805e9a" stroke-width="1.2"/><ellipse cx="17" cy="12.5" rx="3.4" ry="5" transform="rotate(-7 17 12.5)" fill="url(#paw-beans)"/><path d="m16 6 1-1" stroke="white" stroke-width="1.3" stroke-linecap="round"/></g>
      <g class="paw-toe paw-toe-middle-right"><ellipse cx="28" cy="12.5" rx="6" ry="9" transform="rotate(8 28 12.5)" fill="url(#paw-fur)" stroke="#805e9a" stroke-width="1.2"/><ellipse cx="28" cy="13" rx="3.4" ry="4.8" transform="rotate(8 28 13)" fill="url(#paw-beans)"/></g>
      <g class="paw-toe paw-toe-right"><ellipse cx="36.2" cy="20" rx="6" ry="8.5" transform="rotate(27 36.2 20)" fill="url(#paw-fur)" stroke="#805e9a" stroke-width="1.2"/><ellipse cx="35.8" cy="20.3" rx="3.3" ry="4.5" transform="rotate(27 35.8 20.3)" fill="url(#paw-beans)"/></g>
      <g class="paw-palm"><path d="M14 30c1-2 2-5 5-5 2 0 2 2 3 2s2-2 4-2c3 0 4 3 5 5 4 5 2 10-2 10-3 0-4-2-7-2s-4 2-7 2c-4 0-5-5-1-10Z" fill="url(#paw-beans)"/><path d="M17 30c1-1 1-2 2-2" stroke="#fff3fa" stroke-width="1.5" stroke-linecap="round"/></g>
      <path d="m14 43 2 1m13-1-2 1" stroke="#c5abd7" stroke-width="1.2" stroke-linecap="round"/>
    </svg></div>`;
    document.body.append(layer);const paw=layer.firstElementChild;
    let frame=0,x=0,y=0,releaseTimer,pressedAt=0,known=false,inside=false;
    const positionKey='pawcare-pointer-position';
    const native='input:not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit]):not([type=reset]),textarea,[contenteditable]:not([contenteditable=false]),iframe,[data-native-cursor],:disabled,[aria-disabled=true]';
    function hide(){root.classList.remove('has-paw-cursor');paw.classList.remove('is-pressed');clearTimeout(releaseTimer);}
    function position(){frame=0;paw.style.transform=`translate3d(${x-10.4}px,${y-1.9}px,0)`;}
    function remember(){
        try{if(known&&inside)sessionStorage.setItem(positionKey,JSON.stringify({x,y,time:Date.now(),width:innerWidth,height:innerHeight}));else sessionStorage.removeItem(positionKey);}catch(_){}
    }
    function refresh(target=document.elementFromPoint(x,y)){
        if(document.documentElement.classList.contains('paw-is-loading')){hide();return;}
        if(!known||!inside||!mouse.matches||contrast.matches||target?.closest(native)){hide();return;}
        position();root.classList.add('has-paw-cursor');
        paw.classList.toggle('is-hovering',!!target?.closest('a[href],button,select,label,[role=button],summary,.form-check-input'));
    }
    function move(event){
        if(event.pointerType!=='mouse'){inside=false;hide();return;}
        x=event.clientX;y=event.clientY;known=true;inside=true;
        if(!frame)frame=requestAnimationFrame(()=>{frame=0;refresh(event.target);});
    }
    document.addEventListener('pointermove',move,{passive:true});
    document.addEventListener('pointerdown',event=>{
        if(event.pointerType!=='mouse'){inside=false;hide();remember();return;}
        if(event.pointerType==='mouse'){x=event.clientX;y=event.clientY;known=true;inside=true;remember();}
        if(event.button!==0||!root.classList.contains('has-paw-cursor'))return;
        clearTimeout(releaseTimer);pressedAt=performance.now();paw.classList.add('is-pressed');
    },{passive:true});
    window.addEventListener('pointerup',()=>{releaseTimer=setTimeout(()=>paw.classList.remove('is-pressed'),Math.max(0,115-(performance.now()-pressedAt)));},{passive:true});
    window.addEventListener('pointercancel',hide);window.addEventListener('blur',hide);
    document.documentElement.addEventListener('pointerleave',()=>{inside=false;hide();});
    document.addEventListener('visibilitychange',()=>{if(document.hidden)hide();});
    window.addEventListener('pagehide',remember);
    window.addEventListener('pageshow',()=>refresh());
    window.addEventListener('pawcare:loading',()=>refresh());
    window.addEventListener('pawcare:page-ready',()=>refresh());
    mouse.addEventListener('change',hide);contrast.addEventListener('change',hide);
    try{
        const saved=JSON.parse(sessionStorage.getItem(positionKey)||'null');
        if(saved&&Date.now()-saved.time<10000&&saved.width===innerWidth&&saved.height===innerHeight&&Number.isFinite(saved.x)&&Number.isFinite(saved.y)&&saved.x>=0&&saved.y>=0&&saved.x<innerWidth&&saved.y<innerHeight){x=saved.x;y=saved.y;known=true;inside=true;refresh();}
    }catch(_){}
})();
