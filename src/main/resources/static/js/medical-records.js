function resetRecordEditor() {
    editingMedicalId=null;
    document.getElementById('recordEditorTitle').textContent='Create Medical Record';
    for(const id of ['recPetId','recApptId','recDiag','recTreat','recPresc','recWeight','recTemp','recFollowup','recNotes']){
        const el=document.getElementById(id);el.value='';el.disabled=false;
    }
    document.getElementById('recType').value='CONSULTATION';
    document.getElementById('recAttachment').value=''; document.getElementById('existingAttachments').innerHTML='';
    const ctx=document.getElementById('appointmentContext'); if(ctx){ctx.hidden=true;ctx.innerHTML='';}
    document.getElementById('recDate').value=localDate();
    document.getElementById('recFollowup').min=localDate();
}
async function editRecord(id) {
    try {
        const record=await api.get('/medical-records/'+id);
        showSection('addRecord');editingMedicalId=id;
        document.getElementById('recordEditorTitle').textContent='Edit Medical Record #'+id;
        const fields={recPetId:record.petId,recApptId:record.appointmentId,recType:record.recordType||'GENERAL',recDiag:record.diagnosis,recTreat:record.treatment,recPresc:record.prescription,recWeight:record.weightAtVisit,recTemp:record.temperature,recDate:record.recordDate,recFollowup:record.followUpDate,recNotes:record.notes};
        Object.entries(fields).forEach(([key,val])=>document.getElementById(key).value=val??'');
        document.getElementById('recPetId').disabled=true;document.getElementById('recApptId').disabled=true;
        document.getElementById('recFollowup').min=record.followUpDate&&record.followUpDate<localDate()?record.followUpDate:localDate();
        const files=await api.getMedicalAttachments(id);document.getElementById('existingAttachments').innerHTML=files.map(f=>`<button type="button" class="btn btn-sm btn-link p-0" onclick="openSecuredFile('${escapeHtml(f.fileUrl)}','${escapeHtml(f.fileName)}')"><i class="bi bi-paperclip"></i> ${escapeHtml(f.fileName)}</button>`).join('<br>')||'<span class="text-muted">No attachments</span>';
    }catch(e){showToast(e.message,'error');}
}
async function deleteRecord(id,button) {
    if(button.disabled)return;
    let password=null;if(api.getRole()==='VETERINARIAN'){password=await requestPasswordConfirmation('Confirm Medical Record Deletion');if(!password)return;}else if(!await pawConfirm('Delete medical record #'+id+'? This cannot be undone.',{title:'Delete medical record?',type:'danger',confirmText:'Delete Record'}))return;
    button.disabled=true;
    try{await (password?api.deleteConfirmed('/medical-records/'+id,password):api.delete('/medical-records/'+id));await loadRecords();showToast('Medical record deleted.','success');}
    catch(e){showToast(e.message,'error');}finally{button.disabled=false;}
}
if(api.getRole()==='ADMIN') {
    document.querySelector('.sidebar-brand small').textContent='Admin Panel';
    document.querySelector('.dashboard-header h4').textContent='Medical Record Management';
    document.querySelector('.sidebar-nav').insertAdjacentHTML('afterbegin','<div class="nav-item"><a class="nav-link" href="/pages/dashboard/admin.html"><i class="bi bi-arrow-left"></i>Admin Dashboard</a></div>');
}
if(location.hash==='#records')showSection('records');
