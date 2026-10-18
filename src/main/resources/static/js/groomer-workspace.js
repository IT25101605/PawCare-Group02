const groomNextStates={SCHEDULED:['CONFIRMED','CANCELLED'],CONFIRMED:['IN_PROGRESS','CANCELLED'],IN_PROGRESS:['COMPLETED','CANCELLED'],COMPLETED:[],CANCELLED:[]};
const groomStateLabels={SCHEDULED:'Scheduled',CONFIRMED:'Confirmed',IN_PROGRESS:'In progress',COMPLETED:'Completed',CANCELLED:'Cancelled'};
function groomStatusControl(g) {
    if(['COMPLETED','CANCELLED'].includes(g.status))return '<span class="small text-muted d-block">Visit closed</span>';
    if(g.status==='IN_PROGRESS')return `<span class="groom-progress-label">${escapeHtml(g.progressLabel||'In progress')}</span>`;
    return g.status==='SCHEDULED'
      ? `<button class="btn btn-sm btn-pawcare" onclick="upd(${g.id},'CONFIRMED')"><i class="bi bi-check-circle me-1"></i>Accept booking</button>`
      : `<button class="btn btn-sm btn-pawcare" onclick="upd(${g.id},'IN_PROGRESS')">Start visit</button>`;
}
let groomDayRequest=0;
async function loadGroomerDay() {
    const date=document.getElementById('groomQueueDate').value;
    const generation=++groomDayRequest,queue=document.getElementById('groomDayQueue'),stats=document.getElementById('groomDayStats');
    if(!date){queue.innerHTML='<p class="text-muted">Choose a date to see your visits.</p>';stats.innerHTML='';return;}
    queue.innerHTML='<p role="status" class="text-muted">Loading your visits…</p>';
    try {
        const visits=await api.get('/grooming/appointments/groomer/day?date='+encodeURIComponent(date));
        if(generation!==groomDayRequest)return;
        const remaining=visits.filter(g=>!['COMPLETED','CANCELLED'].includes(g.status));
        const minutes=remaining.reduce((sum,g)=>sum+Number(g.totalDurationMinutes||0),0);
        stats.innerHTML=`<div class="groom-visit-stat"><strong>${visits.length}</strong><span>Visits on this day</span></div><div class="groom-visit-stat"><strong>${minutes}<small class="fs-6"> min</small></strong><span>Planned time remaining</span></div><div class="groom-visit-stat"><strong>${visits.filter(g=>g.status==='COMPLETED').length}</strong><span>Visits completed</span></div>`;
        queue.innerHTML=visits.map(g=>`<article class="groom-queue-card"><div class="groom-queue-time">${escapeHtml((g.appointmentTime||'').slice(0,5))}<small>${g.totalDurationMinutes} min</small></div><div class="groom-queue-content"><div class="d-flex flex-wrap gap-2 align-items-center"><h3 class="mb-0">${escapeHtml(g.petName)}</h3>${statusBadge(g.status)}</div><p>${escapeHtml(g.serviceName)} · ${escapeHtml(g.ownerName)}</p>${groomingComfortBadge(g.comfortProfile)}${g.specialInstructions?`<p class="groom-preserve-lines">${escapeHtml(g.specialInstructions)}</p>`:''}</div><div>${groomStatusControl(g)}<button class="btn btn-sm btn-pawcare-outline mt-2" onclick="openGroomingVisit(${g.id})">Checklist & report <i class="bi bi-arrow-right"></i></button></div></article>`).join('')||'<div class="card-pawcare p-4 text-center"><i class="bi bi-calendar-heart fs-2"></i><h6 class="mt-2">A little breathing room.</h6><p class="text-muted mb-0">No grooming visits booked for this date.</p></div>';
    }catch(e){if(generation===groomDayRequest){stats.innerHTML='';queue.innerHTML=`<div class="text-danger" role="alert">${escapeHtml(e.message)} <button class="btn btn-sm btn-pawcare-outline" onclick="loadGroomerDay()">Try again</button></div>`;}}
}
document.getElementById('groomQueueDate').value=localDate();
loadGroomerDay();
