/* A public service choice survives sign-in; an appointment still needs confirmation. */
window.GroomingBooking=(()=>{
    const key='pawcare-grooming-booking',maxAge=30*60*1000;
    function read(){
        try{
            const intent=JSON.parse(sessionStorage.getItem(key)||'null');
            if(!intent||!Number.isFinite(intent.createdAt)||Date.now()-intent.createdAt>maxAge||intent.createdAt>Date.now()+60000){sessionStorage.removeItem(key);return null;}
            return intent;
        }catch(_){return null;}
    }
    function clear(){try{sessionStorage.removeItem(key);}catch(_){} }
    function destination(intent){
        const params=new URLSearchParams();
        if(intent.mode==='planner')params.set('grooming','planner');else params.set('grooming','book');
        if(intent.serviceIds.length)params.set('services',intent.serviceIds.join(','));
        if(intent.groomerId)params.set('groomer',String(intent.groomerId));
        // Fixed local path also works when browser storage is unavailable.
        const path='/pages/dashboard/owner.html?'+params+'#grooming';
        return api.isLoggedIn()&&api.getRole()==='PET_OWNER'?path:'/pages/login.html?'+params;
    }
    function captureQuery(){
        const params=new URLSearchParams(location.search),mode=params.get('grooming');
        if(!['book','planner'].includes(mode))return;
        const intent={mode,serviceIds:[...new Set((params.get('services')||'').split(',').map(Number).filter(id=>Number.isSafeInteger(id)&&id>0))].slice(0,8),groomerId:Number(params.get('groomer'))||null,createdAt:Date.now()};
        try{sessionStorage.setItem(key,JSON.stringify(intent));}catch(_){}
        return intent;
    }
    let queryIntent=captureQuery();
    function current(){return read()||(queryIntent&&Date.now()-queryIntent.createdAt<=maxAge?queryIntent:null);}
    let starting=false;
    window.addEventListener('pageshow',()=>{starting=false;});
    async function start({serviceId,groomerId,mode='book'}={}){
        if(starting)return;
        if(api.isLoggedIn()&&api.getRole()!=='PET_OWNER'){
            starting=true;
            const proceed=await pawDialog('Book for your pet using a pet owner account. Your service choice will be kept while you sign in.',{title:'Let’s book their grooming visit',confirmText:'Sign in as pet owner',cancelText:'Keep browsing',type:'info'});
            starting=false;if(!proceed)return;
            api.clearAuth();
        }
        starting=true;
        const intent={mode,serviceIds:serviceId?[Number(serviceId)]:[],groomerId:groomerId?Number(groomerId):null,createdAt:Date.now()};
        try{sessionStorage.setItem(key,JSON.stringify(intent));}catch(_){}
        const next=destination(intent);
        window.pawcarePageLoading?.show(new URL(next,location.origin));
        location.href=next;
    }
    function afterAuth(role,fallback){
        const intent=current();
        if(role==='PET_OWNER'&&intent)return destination(intent);
        if(role!=='PET_OWNER'){clear();queryIntent=null;}
        return fallback;
    }
    function openPlanner(){
        const panel=document.getElementById('groomPlanner');if(!panel)return;
        panel.open=true;
        const target=document.getElementById('groomPet').value?panel:document.getElementById('groomPet');
        target.scrollIntoView({block:'center',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});
        if(target.tagName==='SELECT')target.focus({preventScroll:true});
    }
    let addingPet=false;
    function addPet(){
        const intent={mode:document.getElementById('groomPlanner').open?'planner':'book',serviceIds:selectedGroomServiceIds(),groomerId:Number(document.getElementById('groomGroomer').value)||null,createdAt:Date.now()};
        queryIntent=intent;try{sessionStorage.setItem(key,JSON.stringify(intent));}catch(_){}
        addingPet=true;showSection('addPet');
    }
    function petAdded(petId){
        if(!addingPet)return false;
        addingPet=false;const intent=current();
        if(intent){intent.petId=Number(petId);queryIntent=intent;try{sessionStorage.setItem(key,JSON.stringify(intent));}catch(_){} }
        return true;
    }
    function apply(){
        const intent=current();if(!intent||api.getRole()!=='PET_OWNER')return;
        const requested=new Set((intent.serviceIds||[]).map(Number)),available=[...document.querySelectorAll('.groom-service-check')];
        available.forEach(input=>input.checked=requested.has(Number(input.value)));
        const selected=available.filter(input=>input.checked).length,missing=requested.size-selected;
        const groomer=document.getElementById('groomGroomer');
        const foundGroomer=intent.groomerId&&[...groomer.options].some(option=>Number(option.value)===intent.groomerId);
        if(foundGroomer){groomer.value=String(intent.groomerId);updateSelectedGroomer();}
        if(intent.petId){const pet=document.getElementById('groomPet');if([...pet.options].some(option=>Number(option.value)===intent.petId))pet.value=String(intent.petId);}
        updateGroomServiceSummary();
        const notice=document.getElementById('groomBookingNotice');
        const parts=[selected?selected+' service'+(selected===1?'':'s')+' selected.':'Let’s plan their visit.',foundGroomer?'Your groomer is selected.':'',missing?'A chosen service is no longer available. Please choose another below.':'',intent.groomerId&&!foundGroomer?'That groomer is no longer available. Please choose another.':'','Choose your pet, review the services and pick an available time.'];
        notice.querySelector('[data-booking-message]').textContent=parts.filter(Boolean).join(' ');notice.hidden=false;
        clear();queryIntent=null;
        if(location.search.includes('grooming='))history.replaceState(null,'',location.pathname+'#grooming');
        if(intent.mode==='planner')document.getElementById('groomPlanner').open=true;
    }
    document.addEventListener('click',event=>{
        const action=event.target.closest('[data-groom-book]');if(!action)return;
        if(event.button!==0||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
        event.preventDefault();start({mode:action.dataset.groomBook||'book',serviceId:action.dataset.serviceId,groomerId:action.dataset.groomerId});
    });
    // Keep sign-up and sign-in links in the same booking journey, without arbitrary redirects.
    if(current()){
        const intent=current(),params=new URLSearchParams({grooming:intent.mode});
        if(intent.serviceIds?.length)params.set('services',intent.serviceIds.join(','));
        if(intent.groomerId)params.set('groomer',String(intent.groomerId));
        document.querySelectorAll('a[href="/pages/register.html"],a[href="/pages/login.html"]').forEach(link=>link.href=link.getAttribute('href')+'?'+params);
        if(/\/(login|register)\.html$/.test(location.pathname)){
            const form=document.querySelector('.auth-form-card form');
            if(form){const note=document.createElement('p');note.className='alert alert-light small';note.setAttribute('role','status');note.textContent='Your grooming choices are saved. Sign in or create a pet owner account to finish booking.';form.before(note);}
        }
    }
    return {start,afterAuth,apply,openPlanner,addPet,petAdded};
})();
