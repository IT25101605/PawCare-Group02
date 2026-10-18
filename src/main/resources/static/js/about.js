(() => {
    const metrics=document.getElementById('aboutMetrics');
    const refresh=document.getElementById('aboutRefresh');
    const status=document.getElementById('aboutLiveStatus');
    const error=document.getElementById('aboutDataError');
    const updated=document.getElementById('aboutUpdated');
    const format=new Intl.NumberFormat('en');
    let pending=false,timer,lastUpdated=null;
    async function loadStats() {
        if(pending || document.hidden)return;
        pending=true;refresh.disabled=true;metrics.setAttribute('aria-busy','true');
        try {
            const data=await api.get('/site/stats');
            for(const key of ['pets','veterinarians','groomers','products','groomingServices'])document.getElementById('about-'+key).textContent=format.format(data[key]);
            document.getElementById('about-rating').textContent=data.reviewCount && data.averageRating!==null ? Number(data.averageRating).toFixed(1)+'★':'—';
            document.getElementById('about-review-caption').textContent=data.reviewCount ? 'From '+format.format(data.reviewCount)+' published review'+(data.reviewCount===1?'':'s') : 'No customer reviews yet';
            lastUpdated=new Date(data.updatedAt);
            status.textContent='Live counts';status.classList.remove('is-unavailable');
            updated.textContent='Updated '+lastUpdated.toLocaleTimeString(undefined,{hour:'2-digit',minute:'2-digit'})+' · Counts refresh every 30 seconds.';
            error.hidden=true;refresh.textContent='Refresh';
        }catch(exception) {
            status.textContent='Refresh unavailable';status.classList.add('is-unavailable');
            error.textContent=lastUpdated?'Could not refresh the counts. The last available numbers are shown. Try again.':'Could not load the live counts. Try again.';
            error.hidden=false;refresh.textContent='Try again';
        }finally{pending=false;refresh.disabled=false;metrics.setAttribute('aria-busy','false');}
    }
    function start(){clearInterval(timer);loadStats();timer=setInterval(loadStats,30000);}
    refresh.addEventListener('click',loadStats);
    document.addEventListener('visibilitychange',()=>{if(document.hidden)clearInterval(timer);else start();});
    window.addEventListener('pagehide',()=>clearInterval(timer));
    window.addEventListener('pageshow',event=>{if(event.persisted)start();});
    start();
})();
