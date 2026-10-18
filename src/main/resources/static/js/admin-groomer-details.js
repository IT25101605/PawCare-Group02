let adminGroomerEditorVersion=0;
async function editGroomerDetails(id) {
    const version=++adminGroomerEditorVersion;
    let modal=document.getElementById('adminGroomerModal');
    if(!modal){
        modal=document.createElement('div');modal.id='adminGroomerModal';modal.className='modal fade';modal.tabIndex=-1;modal.setAttribute('aria-labelledby','adminGroomerTitle');document.body.append(modal);
        modal.addEventListener('hide.bs.modal',event=>{if(modal.querySelector('form')?.dataset.saving==='true')event.preventDefault();});
        modal.addEventListener('show.bs.modal',()=>modal.dataset.ready='false');modal.addEventListener('shown.bs.modal',()=>modal.dataset.ready='true');
        modal.addEventListener('hidden.bs.modal',()=>modal.dataset.ready='false');
    }
    modal.innerHTML=`<div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h5 class="modal-title" id="adminGroomerTitle">Groomer details</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><form id="adminGroomerForm"><div class="modal-body"><p class="small text-muted" id="adminGroomerName">Loading professional details…</p><div class="row g-3">${GroomerFields.fields('adminGroomer')}</div><p id="adminGroomerError" class="text-danger mt-3 mb-0" role="alert"></p></div><div class="modal-footer"><button type="button" class="btn btn-pawcare-outline" data-bs-dismiss="modal">Cancel</button><button class="btn btn-pawcare" type="submit" disabled>Save details</button></div></form></div></div>`;
    GroomerFields.attach(GroomerFields.ids('adminGroomer'));
    const form=document.getElementById('adminGroomerForm'),button=form.querySelector('[type=submit]');
    configureForm(form);bootstrap.Modal.getOrCreateInstance(modal).show();
    form.addEventListener('submit',async event=>{
        event.preventDefault();if(button.disabled)return;button.disabled=true;form.dataset.saving='true';document.getElementById('adminGroomerError').textContent='';
        try{const saved=await api.put('/users/'+id+'/groomer-details',GroomerFields.read('adminGroomer'));GroomerFields.remember(saved);form.dataset.saving='false';const close=()=>bootstrap.Modal.getInstance(modal).hide();if(modal.dataset.ready==='true')close();else modal.addEventListener('shown.bs.modal',close,{once:true});showToast('Groomer details saved.','success');}
        catch(error){document.getElementById('adminGroomerError').textContent=error.message;}
        finally{button.disabled=false;form.dataset.saving='false';}
    });
    try {
        const [user,details]=await Promise.all([api.get('/users/'+id),api.get('/users/'+id+'/groomer-details')]);
        if(version!==adminGroomerEditorVersion)return;
        document.getElementById('adminGroomerName').textContent=[user.firstName,user.lastName].filter(Boolean).join(' ')+' · Choose suggestions or type the groomer’s own details.';
        GroomerFields.fill('adminGroomer',details);button.disabled=false;
    }catch(error){if(version===adminGroomerEditorVersion)document.getElementById('adminGroomerError').textContent='Could not load details. Close and reopen this editor to retry. '+error.message;}
}
