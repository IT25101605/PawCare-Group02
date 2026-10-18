/* Owner planner and shared visit handover. All catalogue and owner text is escaped. */
const groomingComfortLabels = {CALM:'Comfortable with grooming', FIRST_VISIT:'First grooming visit', NERVOUS:'Needs a gentle introduction'};
function groomingComfortBadge(profile) {
    return profile ? `<span class="groom-comfort ${profile==='CALM'?'calm':''}"><i class="bi bi-heart"></i>${escapeHtml(groomingComfortLabels[profile]||'Comfort preference')}</span>` : '';
}
let groomingPlan = null;
let groomingPlanGeneration = 0;
function invalidateGroomingPlan() {
    groomingPlan = null;
    groomingPlanGeneration++;
    const result=document.getElementById('groomPlanResult');
    if(result) result.innerHTML='';
}
async function buildGroomingPlan() {
    const petId=Number(document.getElementById('groomPet').value);
    if(!petId){showToast('Choose your pet above first.','warning');document.getElementById('groomPet').focus();return;}
    const budget=document.getElementById('groomBudget'),time=document.getElementById('groomMaxMinutes');
    if(!budget.reportValidity()||!time.reportValidity())return;
    if(!document.getElementById('groomComfort').value){document.getElementById('groomPlanner').open=true;showToast('Choose your pet’s comfort level to personalize the plan.','info');document.getElementById('groomComfort').focus();return;}
    const generation=++groomingPlanGeneration;
    const button=document.getElementById('groomPlanButton'),result=document.getElementById('groomPlanResult');
    button.disabled=true;button.textContent='Finding your fit…';groomingPlan=null;
    result.innerHTML='<p role="status">Matching your preferences with available services…</p>';
    try {
        const plan=await api.post('/grooming/plan',{petId,budget:Number(budget.value),maxMinutes:Number(time.value),goal:document.getElementById('groomGoal').value,comfort:document.getElementById('groomComfort').value});
        if(generation!==groomingPlanGeneration)return;
        groomingPlan=plan;
        result.innerHTML=`<h6 class="fw-bold">${plan.services.length?'A little care, chosen for':'Let’s adjust the plan for'} ${escapeHtml(plan.petName)}</h6>
            ${plan.services.map(s=>`<div class="groom-plan-item"><div><strong>${escapeHtml(s.name)}</strong><small>${escapeHtml(s.reason)}</small></div><span>${formatCurrency(s.price)} · ${s.durationMinutes} min</span></div>`).join('')}
            <ul class="groom-plan-reasons">${plan.explanations.map(reason=>`<li>${escapeHtml(reason)}</li>`).join('')}</ul>
            ${plan.services.length?`<div class="d-flex flex-wrap justify-content-between align-items-center gap-3"><strong>${formatCurrency(plan.totalPrice)} · ${plan.totalMinutes} min total</strong><button type="button" class="btn btn-pawcare" onclick="applyGroomingPlan()">Use these services <i class="bi bi-arrow-down-short"></i></button></div>`:''}`;
    }catch(e){if(generation===groomingPlanGeneration)result.innerHTML=`<p class="text-danger" role="alert">${escapeHtml(e.message||'Unable to build a plan. Please try again.')}</p>`;}
    finally{button.disabled=false;button.innerHTML='<i class="bi bi-stars me-1"></i> Find my fit';}
}
function applyGroomingPlan() {
    if(!groomingPlan?.services.length)return;
    const ids=new Set(groomingPlan.services.map(s=>Number(s.id)));
    const inputs=[...document.querySelectorAll('.groom-service-check')];
    if([...ids].some(id=>!inputs.some(input=>Number(input.value)===id))){showToast('The catalogue has changed. Reload the services and try again.','warning');return;}
    inputs.forEach(input=>input.checked=ids.has(Number(input.value)));
    updateGroomServiceSummary();
    document.getElementById('groomServiceSummary').scrollIntoView({behavior:'smooth',block:'center'});
    showToast('Services selected. Choose a groomer, date and available time to book.','success');
}
['groomPet','groomBudget','groomMaxMinutes','groomGoal','groomComfort'].forEach(id=>document.getElementById(id)?.addEventListener('input',invalidateGroomingPlan));


const groomingFinishLabels={AS_DISCUSSED:'Discuss with the groomer',NATURAL_TIDY:'Natural tidy-up',SHORT_NEAT:'Short & neat',KEEP_LENGTH:'Keep the coat length'};
let groomVisitModal=null,groomVisitData=null,groomVisitPoll=null,groomVisitRequest=0;
async function openGroomingVisit(id) {
    let host=document.getElementById('groomVisitModal');
    if(!host){
        host=document.createElement('div');host.id='groomVisitModal';host.className='modal fade';host.tabIndex=-1;
        host.setAttribute('aria-labelledby','groomVisitTitle');
        host.innerHTML='<div class="modal-dialog modal-dialog-centered modal-lg"><div class="modal-content"><div class="modal-header"><h5 class="modal-title" id="groomVisitTitle">Grooming visit</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close visit"></button></div><div class="modal-body" id="groomVisitBody"></div></div></div>';
        document.body.appendChild(host);groomVisitModal=new bootstrap.Modal(host);
        host.addEventListener('hidden.bs.modal',()=>{clearInterval(groomVisitPoll);groomVisitPoll=null;groomVisitRequest++;groomVisitData=null;});
    }
    clearInterval(groomVisitPoll);groomVisitData=null;
    const body=document.getElementById('groomVisitBody');
    body.innerHTML='<p role="status">Loading visit…</p>';groomVisitModal.show();
    await refreshGroomingVisit(id);
    if(api.getRole()==='PET_OWNER'&&groomVisitData&&host.classList.contains('show'))groomVisitPoll=setInterval(()=>{
        if(!document.hidden && groomVisitData && !['COMPLETED','CANCELLED'].includes(groomVisitData.status))refreshGroomingVisit(id,true);
    },20000);
}
async function refreshGroomingVisit(id,quiet=false) {
    const request=++groomVisitRequest;
    try {
        const g=await api.get('/grooming/appointments/'+id);
        if(request!==groomVisitRequest)return;
        groomVisitData=g;renderGroomingVisit(g);
    }catch(e){if(request===groomVisitRequest&&!quiet)document.getElementById('groomVisitBody').innerHTML=`<p class="text-danger" role="alert">${escapeHtml(e.message)}</p>`;}
}
function renderGroomingVisit(g) {
    const expanded=new Map([...document.querySelectorAll('#groomVisitBody details')].map(detail=>[detail.querySelector('summary')?.textContent,detail.open]));
    const staff=['GROOMER','ADMIN'].includes(api.getRole()),active=g.status==='IN_PROGRESS';
    const tasks=g.visitTasks||[],done=(g.completedTasks||[]).length;
    const tracking=g.taskTrackingEnabled,closed=['COMPLETED','CANCELLED'].includes(g.status);
    const taskRows=tasks.map((task,i)=>staff&&active?`<label class="groom-task"><input type="checkbox" class="form-check-input" ${task.completed?'checked':''} ${i!==done&&i!==done-1?'disabled':''} onchange="this.disabled=true;updateGroomVisitTask(${i},this.checked)"><span>${escapeHtml(task.label)}</span>${task.completed?'<small>Done</small>':''}</label>`:`<div class="groom-task ${task.completed?'done':''}"><i class="bi ${task.completed?'bi-check-circle-fill':'bi-circle'}" aria-hidden="true"></i><span>${escapeHtml(task.label)}</span></div>`).join('');
    document.getElementById('groomVisitBody').innerHTML=`
      <div class="d-flex justify-content-between flex-wrap gap-2"><div><h4>${escapeHtml(g.petName)}</h4><p class="text-muted mb-1">${escapeHtml(g.serviceName)}</p></div>${statusBadge(g.status)}</div>
      <div class="groom-live-stage" aria-live="polite"><i class="bi ${g.status==='COMPLETED'?'bi-check-circle':'bi-scissors'}" aria-hidden="true"></i><div><small>Visit progress</small><strong>${escapeHtml(g.progressLabel||groomStateFallback(g.status))}</strong>${!staff&&!closed?'<small>Updates automatically while this visit is open.</small>':''}</div></div>
      <div class="d-flex flex-wrap gap-3 small mb-3"><span>${formatDate(g.appointmentDate)} · ${escapeHtml((g.appointmentTime||'').slice(0,5))}</span><span>${g.totalDurationMinutes} min</span><strong>${formatCurrency(g.totalPrice)}</strong></div>
      <p class="small text-muted">Groomer: ${escapeHtml(g.groomerName||'Unassigned')}</p>
      ${staff&&g.status==='SCHEDULED'?`<button class="btn btn-pawcare mb-3" onclick="acceptGroomVisit(${g.id},this)">Accept booking</button>`:''}
      ${staff&&g.status==='CONFIRMED'?`<button class="btn btn-pawcare mb-3" onclick="startGroomVisit(${g.id},this)">Start visit</button>`:''}
      ${g.status!=='CANCELLED'&&(active||g.status==='COMPLETED')?`<${staff?'section':'details'} class="groom-checklist">${staff?'<h6>Visit checklist</h6>':'<summary>View grooming steps</summary>'}${tracking?`<p class="small text-muted mb-2">${done} of ${tasks.length} steps recorded</p>${taskRows}`:'<p class="small text-muted">Detailed steps were not recorded for this earlier visit.</p>'}${staff&&active&&(!tracking||done===tasks.length)?`<button class="btn btn-pawcare mt-3" onclick="finishGroomVisit(${g.id},this)">Ready for pickup <i class="bi bi-check2 ms-1"></i></button>`:''}</${staff?'section':'details'}>`:''}
      <details class="groom-visit-detail"><summary>Preferences & preparation</summary><div class="pt-3">${groomingComfortBadge(g.comfortProfile)}<p class="mt-2"><strong>Preferred finish:</strong> ${escapeHtml(groomingFinishLabels[g.preferredFinish]||'Discuss with the groomer')}</p><p class="groom-preserve-lines mt-2">${escapeHtml(g.specialInstructions||'No special instructions shared.')}</p></div></details>
      ${g.status!=='CANCELLED'?`<details class="groom-visit-detail" ${g.status==='COMPLETED'?'open':''}><summary>Care report</summary><div class="pt-3">${staff&&g.status==='IN_PROGRESS'?`
        <p class="small text-muted">Share grooming observations and agreed care with the owner. Keep observations factual; refer health concerns to a veterinarian.</p>
        <label class="form-label" for="groomVisitNotes">How did the visit go?</label><textarea class="form-control" id="groomVisitNotes" maxlength="2000" rows="3" placeholder="Coat observations, care completed and handling notes…">${escapeHtml(g.notes||'')}</textarea>
        <label class="form-label mt-3" for="groomProductsUsed">Products used (optional)</label><input class="form-control" id="groomProductsUsed" maxlength="500" value="${escapeHtml(g.productsUsed||'')}" placeholder="For example: fragrance-free shampoo">
        <label class="form-label mt-3" for="groomNextVisit">Suggested next visit (optional)</label><input type="date" class="form-control" id="groomNextVisit" value="${escapeHtml(g.nextVisitDate||'')}">
        <button class="btn btn-pawcare mt-3" id="saveGroomNotes" onclick="saveGroomingNotes(${g.id})">Save care report</button>
      `:`<p class="groom-preserve-lines">${escapeHtml(g.notes||'Your groomer has not shared a care report yet.')}</p>${g.productsUsed?`<p class="mt-3"><strong>Products used:</strong> ${escapeHtml(g.productsUsed)}</p>`:''}${g.nextVisitDate?`<div class="groom-next-visit mt-3"><i class="bi bi-calendar-heart"></i><div><small>Suggested next visit</small><strong>${formatDate(g.nextVisitDate)}</strong><small>You can book a time that suits you.</small></div></div>`:''}`}</div></details>`:''}
      ${!staff&&g.status==='COMPLETED'?`<button class="btn btn-pawcare mt-2" onclick="repeatGroomingBooking(${g.id},this)"><i class="bi bi-arrow-repeat me-1"></i>Book this visit again</button>`:''}`;
    document.querySelectorAll('#groomVisitBody details').forEach(detail=>{const label=detail.querySelector('summary')?.textContent;if(expanded.has(label))detail.open=expanded.get(label);});
}
function groomStateFallback(status){return ({SCHEDULED:'Booked',CONFIRMED:'Confirmed',IN_PROGRESS:'In progress',COMPLETED:'Ready for pickup',CANCELLED:'Cancelled'})[status]||status;}
async function acceptGroomVisit(id,button){button.disabled=true;try{await api.updateGrooming(id,{status:'CONFIRMED'});await refreshGroomingVisit(id);if(typeof loadGroomerDay==='function')loadGroomerDay();if(typeof loadMy==='function')loadMy();}catch(e){showToast(e.message,'error');button.disabled=false;}}
async function startGroomVisit(id,button){button.disabled=true;try{await api.updateGrooming(id,{status:'IN_PROGRESS'});await refreshGroomingVisit(id);if(typeof loadGroomerDay==='function')loadGroomerDay();}catch(e){showToast(e.message,'error');button.disabled=false;}}
async function updateGroomVisitTask(index,checked) {
    const g=groomVisitData;if(!g)return;
    const requested=(g.visitTasks||[]).slice(0,checked?index+1:index).map(task=>task.code);
    document.querySelectorAll('#groomVisitBody .groom-task input').forEach(input=>input.disabled=true);
    try{await api.updateGrooming(g.id,{completedTasks:requested});await refreshGroomingVisit(g.id);if(typeof loadGroomerDay==='function')loadGroomerDay();}
    catch(e){showToast(e.message,'error');await refreshGroomingVisit(g.id);}
}
async function finishGroomVisit(id,button) {
    button.disabled=true;
    try{await api.updateGrooming(id,{status:'COMPLETED'});await refreshGroomingVisit(id);if(typeof loadGroomerDay==='function')loadGroomerDay();if(typeof loadMy==='function')loadMy();showToast('Visit completed. The owner can see it is ready for pickup.','success');}
    catch(e){showToast(e.message,'error');button.disabled=false;}
}
async function saveGroomingNotes(id) {
    const button=document.getElementById('saveGroomNotes');button.disabled=true;
    try{
        const next=document.getElementById('groomNextVisit').value;
        await api.updateGrooming(id,{notes:document.getElementById('groomVisitNotes').value.trim(),productsUsed:document.getElementById('groomProductsUsed').value.trim(),nextVisitDate:next||null,clearNextVisitDate:!next});
        showToast('Care report saved for the owner.','success');
    }catch(e){showToast(e.message,'error');}finally{button.disabled=false;}
}
async function repeatGroomingBooking(id,button) {
    if(button)button.disabled=true;
    try {
        const previous=await api.get('/grooming/appointments/'+id);
        if(previous.status!=='COMPLETED')throw new Error('Book again is available after a completed visit.');
        resetAppointmentEditor(true);
        await Promise.all([loadPets(),loadGroomServices(),loadGroomers()]);
        const pet=document.getElementById('groomPet');
        if(![...pet.options].some(option=>Number(option.value)===Number(previous.petId)))throw new Error('This pet is not available to book.');
        pet.value=String(previous.petId);
        const ids=new Set((previous.serviceIds||[previous.serviceId]).map(Number));
        const available=new Set(groomingServices.map(service=>Number(service.id)));
        document.querySelectorAll('.groom-service-check').forEach(input=>input.checked=ids.has(Number(input.value)));
        const groomer=document.getElementById('groomGroomer');
        groomer.value=[...groomer.options].some(option=>Number(option.value)===Number(previous.groomerId))?String(previous.groomerId):'';
        document.getElementById('groomFinish').value=previous.preferredFinish||'AS_DISCUSSED';
        document.getElementById('groomComfort').value=previous.comfortProfile||'';
        document.getElementById('groomInstructions').value=previous.specialInstructions||'';document.getElementById('groomInstructions').dispatchEvent(new Event('input',{bubbles:true}));
        updateGroomServiceSummary();invalidateGroomingPlan();
        document.getElementById('groomEditorTitle').textContent='Book another visit';
        if(groomVisitModal)groomVisitModal.hide();
        document.getElementById('groomEditorTitle').scrollIntoView({behavior:'smooth',block:'start'});
        showToast([...ids].some(key=>!available.has(key))?'Some earlier services are unavailable. Review the selection, then choose a new date and time.':'Your services and preferences are copied. Review current prices and choose a new date and time.','info');
    }catch(e){showToast(e.message,'error');}finally{if(button)button.disabled=false;}
}
