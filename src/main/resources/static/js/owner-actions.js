function resetPetEditor() {
    editingPetId=null;
    editingPetPassword=null;
    document.getElementById('petEditorTitle').textContent='Add New Pet';
    ['petName','petDob','petWeight','petColor','petPhoto','petCustomSpecies','petCustomBreed'].forEach(id=>document.getElementById(id).value='');
    ['petCustomSpecies','petCustomBreed'].forEach(id=>{const e=document.getElementById(id);if(e){e.hidden=true;e.required=false;}});
    document.getElementById('petGender').value='MALE';
    document.getElementById('petPreview').hidden=true;
}
function payMyOrder(id,button) { openPaymentFlow(id,()=>loadOrders()); }
async function editMyPet(id) {
    try {
        const password=await requestPasswordConfirmation('Confirm pet edit','Enter your current password before editing this pet.','Continue to Edit');
        if(!password)return;
        const pet=await api.getPet(id);
        await showSection('addPet');
        editingPetId=id;
        editingPetPassword=password;
        document.getElementById('petEditorTitle').textContent='Edit Pet';
        const fields={petName:pet.name,petDob:pet.dateOfBirth,petGender:pet.gender||'UNKNOWN',petWeight:pet.weight,petColor:pet.color,petSpecies:pet.speciesId};
        Object.entries(fields).forEach(([key,val])=>document.getElementById(key).value=val??'');
        updatePetWeightLimit();
        await loadBreeds();document.getElementById('petBreed').value=pet.breedId??'';
        const preview=document.getElementById('petPreview');preview.hidden=!pet.photo;if(pet.photo)preview.src=pet.photo;
    } catch(e){showToast(e.message,'error');}
}
function ownerAppointmentActions(a,grooming) {
    const editable=['SCHEDULED','CONFIRMED'].includes(a.status);
    const controls=editable?`<button class="btn btn-sm btn-pawcare-outline" onclick="editOwnerAppointment(${a.id},${grooming})">Edit</button> <button class="btn btn-sm btn-outline-danger" onclick="cancelOwnerAppointment(${a.id},${grooming},this)">Cancel</button>`:'';
    const message=!grooming&&a.veterinarianId?` <button class="btn btn-sm btn-pawcare-outline" onclick="openMessageThread(${a.petId},${a.veterinarianId},${a.id},'${encodeURIComponent(a.veterinarianName||'Veterinarian')}')"><i class="bi bi-chat-dots me-1"></i>Message</button>`:'';
    return controls+message||'-';
}
function resetAppointmentEditor(grooming) {
    const prefix=grooming?'groom':'appt';
    if(grooming) editingGroomId=null; else editingApptId=null;
    document.getElementById(prefix+'Pet').disabled=false;
    document.getElementById(prefix+'EditorTitle').textContent=grooming?'Book Grooming':'Book New Appointment';
    document.getElementById(grooming?'resetGroom':'resetAppt').hidden=true;
    if(grooming){
        document.getElementById('groomBookingNotice').hidden=true;
        editingGroomSnapshot=null;clearTimeout(groomingSlotTimer);groomingSlotRequestSeq++;
        document.querySelectorAll('#section-grooming input, #section-grooming select, #section-grooming textarea').forEach(el=>{el.classList.remove('is-invalid','is-valid');el.setCustomValidity('');});document.querySelectorAll('#section-grooming .field-validation-message').forEach(el=>el.textContent='');
        invalidateGroomingPlan();document.getElementById('groomComfort').value='';document.getElementById('groomFinish').value='AS_DISCUSSED';document.querySelectorAll('#section-grooming details').forEach(el=>el.open=false);document.getElementById('groomBookButton').textContent='Book Grooming';
        ['groomDate','groomTime','groomInstructions'].forEach(id=>{const el=document.getElementById(id);if(el)el.value='';});
        document.getElementById('groomInstructions').dispatchEvent(new Event('input',{bubbles:true}));
        document.querySelectorAll('.groom-service-check').forEach(el=>el.checked=false);
        if(typeof updateGroomServiceSummary==='function') updateGroomServiceSummary(false);
        const time=document.getElementById('groomTime');
        if(time){time.disabled=true;time.innerHTML='<option value="">Select services, groomer and date first</option>';}
    } else {
        ['apptVet','apptDate','apptReason'].forEach(id=>{const el=document.getElementById(id);if(el)el.value='';});
        const time=document.getElementById('apptTime');
        if(time){time.disabled=true;time.innerHTML='<option value="">Select veterinarian and date first</option>';}
    }
}
async function editOwnerAppointment(id,grooming) {
    try {
        const a=await api.get((grooming?'/grooming/appointments/':'/appointments/')+id);
        if(!['SCHEDULED','CONFIRMED'].includes(a.status))throw Error('Only scheduled or confirmed bookings can be edited.');
        const prefix=grooming?'groom':'appt';
        if(grooming){await Promise.all([loadGroomServices(),loadGroomers()]);editingGroomId=id;}else{await loadVets();editingApptId=id;}
        document.getElementById(prefix+'Pet').value=a.petId;
        document.getElementById(prefix+'Pet').disabled=true;
        document.getElementById(prefix+'Date').value=a.appointmentDate;
        document.getElementById(prefix+'EditorTitle').textContent='Edit Appointment #'+id;
        document.getElementById(grooming?'resetGroom':'resetAppt').hidden=false;
        if(grooming){
            editingGroomSnapshot=a;invalidateGroomingPlan();document.getElementById('groomComfort').value=a.comfortProfile||'';document.getElementById('groomFinish').value=a.preferredFinish||'AS_DISCUSSED';document.getElementById('groomBookButton').textContent='Save changes';
            const selected=new Set((a.serviceIds&&a.serviceIds.length?a.serviceIds:[a.serviceId]).filter(Boolean).map(Number));
            document.querySelectorAll('.groom-service-check').forEach(el=>el.checked=selected.has(Number(el.value)));
            if(document.getElementById('groomInstructions')){document.getElementById('groomInstructions').value=a.specialInstructions||'';document.getElementById('groomInstructions').dispatchEvent(new Event('input',{bubbles:true}));}
            document.getElementById('groomGroomer').value=a.groomerId??'';updateSelectedGroomer();
            if(typeof updateGroomServiceSummary==='function')updateGroomServiceSummary(false);
            await loadGroomingSlots();
        } else {
            document.getElementById('apptVet').value=a.veterinarianId;document.getElementById('apptReason').value=a.reason;
            await loadAppointmentSlots();
        }
        const timeSelect=document.getElementById(prefix+'Time');
        const matchingTime=[...timeSelect.options].find(o=>o.value.slice(0,5)===a.appointmentTime?.slice(0,5));
        if(matchingTime)timeSelect.value=matchingTime.value;
        else if(!grooming && a.appointmentTime){timeSelect.add(new Option(a.appointmentTime,a.appointmentTime));timeSelect.value=a.appointmentTime;}
        document.getElementById(prefix+'Date').focus();
    }catch(e){showToast(e.message,'error');}
}
async function cancelOwnerAppointment(id,grooming,button) {
    if(button.disabled || !await pawConfirm('Cancel appointment #'+id+'?',{title:'Cancel appointment?',type:'warning',confirmText:'Continue'}))return;
    const password=await requestPasswordConfirmation('Confirm cancellation','Enter your current password to cancel this appointment.','Cancel Appointment');
    if(!password)return;
    button.disabled=true;
    try {
        await (grooming?api.updateGroomingConfirmed:api.updateAppointmentConfirmed)(id,{status:'CANCELLED'},password);
        resetAppointmentEditor(grooming);
        await (grooming?loadGroom:loadAppts)();showToast('Appointment cancelled.','success');
    }catch(e){showToast(e.message,'error');}finally{button.disabled=false;}
}
