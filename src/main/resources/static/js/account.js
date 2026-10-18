document.body.insertAdjacentHTML('beforeend', `<div class="modal fade" id="accountModal" tabindex="-1" aria-labelledby="accountTitle"><div class="modal-dialog modal-lg"><div class="modal-content"><div class="modal-header"><h5 id="accountTitle" class="modal-title">My Account</h5><button class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><div id="accountDetails"></div><div id="profileEditor" hidden></div><hr><h6>Change Password</h6><form id="passwordForm"><div class="mb-3"><label for="currentPassword" class="form-label">Current password</label><input class="form-control form-control-pawcare" id="currentPassword" type="password" autocomplete="current-password" required maxlength="72"></div><div class="mb-3"><label for="newPassword" class="form-label">New password</label><input class="form-control form-control-pawcare" id="newPassword" type="password" autocomplete="new-password" required minlength="8" maxlength="72"></div><div class="mb-3"><label for="repeatPassword" class="form-label">Confirm new password</label><input class="form-control form-control-pawcare" id="repeatPassword" type="password" autocomplete="new-password" required minlength="8" maxlength="72"></div><p class="small text-muted">8–72 characters: uppercase, lowercase, a number and a symbol; no spaces.</p><button id="passwordButton" class="btn btn-pawcare" type="submit">Change Password</button><p id="passwordResult" role="status" class="mt-3"></p></form></div></div></div></div>`);

function accountAvatar(u, size='lg') {
    const name=((u?.firstName||'')+' '+(u?.lastName||'')).trim() || 'PawCare user';
    const initials=((u?.firstName||'P').charAt(0)+(u?.lastName||'C').charAt(0)).toUpperCase();
    if (u?.profileImage) return `<img class="pc-account-avatar pc-account-avatar-${size}" src="${escapeHtml(u.profileImage)}" alt="${escapeHtml(name)} profile photo" data-initials="${escapeHtml(initials)}" onerror="accountAvatarFallback(this)">`;
    return `<div class="pc-account-avatar pc-account-avatar-${size} pc-account-avatar-fallback" aria-label="${escapeHtml(name)}">${escapeHtml(initials)}</div>`;
}
function accountAvatarFallback(image) {
    const fallback=document.createElement('div');fallback.className=image.className+' pc-account-avatar-fallback';
    fallback.setAttribute('aria-label',image.alt);fallback.textContent=image.dataset.initials||'PC';image.replaceWith(fallback);
}

async function openAccount() {
    document.getElementById('passwordForm').reset();
    document.getElementById('passwordResult').textContent='';
    document.getElementById('accountDetails').hidden=false;
    document.getElementById('accountDetails').textContent='Loading account...';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('accountModal')).show();
    try {
        const u=await api.me();
        const photoHint=['PET_OWNER','VETERINARIAN','GROOMER'].includes(u.role)?`<p class="small text-muted mb-0"><i class="bi bi-camera me-1"></i>${u.role==='VETERINARIAN'?'Your photo is shown on the Veterinarians page and in your account.':u.role==='GROOMER'?'Your photo is shown to pet owners when they choose a groomer.':'Your photo personalizes your PawCare account.'}</p>`:'';
        document.getElementById('accountDetails').innerHTML=`<div class="pc-account-summary">${accountAvatar(u)}<div><h5 class="mb-1">${escapeHtml((u.firstName||'')+' '+(u.lastName||''))}</h5><div class="text-muted small">${escapeHtml((u.role||'').replaceAll('_',' '))}</div>${photoHint}</div></div><dl class="pc-account-details">${[['Email',u.email],['Phone',u.phone],['Address',[u.address,u.city,u.state,u.zipCode].filter(Boolean).join(', ')]].map(([k,v])=>'<div><dt>'+k+'</dt><dd>'+escapeHtml(v||'-')+'</dd></div>').join('')}</dl><button class="btn btn-pawcare-outline" onclick="editProfile()"><i class="bi bi-pencil me-1"></i>Edit Profile</button>`;
        window.accountUser=u;
        document.getElementById('profileEditor').hidden=true;
    } catch(e){document.getElementById('accountDetails').textContent=e.message;}
}

document.getElementById('passwordForm').addEventListener('submit',async event=>{
    event.preventDefault();
    const currentPassword=document.getElementById('currentPassword').value,newPassword=document.getElementById('newPassword').value;
    const result=document.getElementById('passwordResult'),btn=document.getElementById('passwordButton');
    if(btn.disabled)return;
    if(!/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\s])\S{8,72}$/.test(newPassword)){result.textContent='Use 8–72 characters with uppercase, lowercase, a number and a symbol; no spaces.';return;}
    if(newPassword!==document.getElementById('repeatPassword').value){result.textContent='New passwords do not match.';return;}
    btn.disabled=true;
    try{const r=await api.put('/auth/change-password',{currentPassword,newPassword});result.textContent=r.message;event.target.reset();}
    catch(e){result.textContent=e.message;}finally{btn.disabled=false;}
});

window.accountUser=null;
function editProfile(){
    const u=window.accountUser;if(!u)return;
    const e=document.getElementById('profileEditor');e.hidden=false;document.getElementById('accountDetails').hidden=true;
    const profilePhoto=['PET_OWNER','VETERINARIAN','GROOMER'].includes(u.role)?`<div class="col-12"><div class="pc-profile-photo-editor"><div id="accountPhotoPreview">${accountAvatar(u,'md')}</div><div class="flex-grow-1"><label for="profileImage" class="form-label fw-semibold">Profile photo (optional)</label><input id="profileImage" class="form-control" type="file" accept="image/jpeg,image/png" onchange="previewAccountPhoto(this)"><small class="text-muted d-block mt-1">JPEG or PNG, up to 5 MB.${u.role==='VETERINARIAN'?' This photo is also shown on the Veterinarians page.':u.role==='GROOMER'?' This photo is also shown with your groomer profile.':''}</small>${u.profileImage?'<button type="button" class="btn btn-sm btn-outline-danger mt-2" onclick="removeProfilePhoto()"><i class="bi bi-trash me-1"></i>Remove current photo</button>':''}</div></div></div>`:'';
    e.innerHTML=`<h6>Edit Profile</h6><form id="profileForm"><div class="row g-3">${profilePhoto}<div class="col-6"><label class="form-label">First Name</label><input id="profileFirstName" class="form-control form-control-pawcare" required maxlength="100" value="${escapeHtml(u.firstName||'')}"></div><div class="col-6"><label class="form-label">Last Name</label><input id="profileLastName" class="form-control form-control-pawcare" required maxlength="100" value="${escapeHtml(u.lastName||'')}"></div><div class="col-12"><label class="form-label">Phone</label><input id="profilePhone" class="form-control form-control-pawcare" pattern="[0-9]{10}" maxlength="10" value="${escapeHtml(u.phone||'')}"></div><div class="col-12"><label class="form-label">Address</label><input id="profileAddress" class="form-control form-control-pawcare" minlength="5" maxlength="500" value="${escapeHtml(u.address||'')}"></div><div class="col-6"><label class="form-label">City</label><input id="profileCity" class="form-control form-control-pawcare" maxlength="100" value="${escapeHtml(u.city||'')}"></div><div class="col-6"><label class="form-label">State</label><input id="profileState" class="form-control form-control-pawcare" maxlength="100" value="${escapeHtml(u.state||'')}"></div><div class="col-6"><label class="form-label">Zip Code</label><input id="profileZip" class="form-control form-control-pawcare" maxlength="20" value="${escapeHtml(u.zipCode||'')}"></div></div><div class="mt-3"><button class="btn btn-pawcare" type="submit">Save Profile</button> <button class="btn btn-pawcare-outline" type="button" onclick="cancelProfileEdit()">Cancel</button></div><p id="profileResult" class="mt-2" role="status"></p></form>`;
    document.getElementById('profileForm').addEventListener('submit',saveProfile);
    if(u.role==='GROOMER') {
        const row=document.querySelector('#profileForm .row');
        row.insertAdjacentHTML('beforeend',`<div class="col-12"><hr><strong>Professional groomer details</strong><p class="small text-muted">Help pet owners choose the right care. Pick suggestions or type your own details.</p></div>${GroomerFields.fields('groomer')}`);
        GroomerFields.attach(GroomerFields.ids('groomer'));
        const professionalForm=document.getElementById('profileForm');
        const professionalSave=e.querySelector('#profileForm button[type=submit]');
        professionalSave.disabled=true;
        api.get('/users/me/groomer-details').then(p=>{
            if(document.getElementById('profileForm')!==professionalForm)return;
            document.getElementById('groomerOrganization').value=p.organization||'';
            document.getElementById('groomerQualification').value=p.qualification||'';
            document.getElementById('groomerSpecialty').value=p.specialty||'';
            document.getElementById('groomerExperience').value=p.experienceYears??'';GroomerFields.remember(p);
            professionalSave.disabled=false;
        }).catch(err=>{document.getElementById('profileResult').textContent='Could not load groomer details. Please reopen Edit Profile: '+err.message;});
    }
}

function previewAccountPhoto(input){
    const file=input.files?.[0];if(!file)return;
    if(file.size>5*1024*1024){showToast('Profile image must be 5 MB or smaller.','error');input.value='';return;}
    const url=URL.createObjectURL(file);
    document.getElementById('accountPhotoPreview').innerHTML=`<img class="pc-account-avatar pc-account-avatar-md" src="${url}" alt="Selected profile photo">`;
}

async function removeProfilePhoto(){
    if(!await pawConfirm('Remove your current profile photo?',{title:'Remove profile photo?',type:'warning',confirmText:'Remove Photo'}))return;
    try{const updated=await api.removeMyProfileImage();window.accountUser=updated;showToast('Profile photo removed.','success');await openAccount();}
    catch(e){showToast(e.message,'error');}
}

function cancelProfileEdit(){document.getElementById('profileEditor').hidden=true;document.getElementById('accountDetails').hidden=false;}

async function saveProfile(event){
    event.preventDefault();if(!event.target.reportValidity())return;
    const btn=event.submitter||event.target.querySelector('[type=submit]');if(btn.disabled)return;btn.disabled=true;
    try{
        const u=window.accountUser;
        await api.put('/users/'+u.id,{firstName:value('profileFirstName'),lastName:value('profileLastName'),phone:value('profilePhone')||null,address:value('profileAddress')||null,city:value('profileCity')||null,state:value('profileState')||null,zipCode:value('profileZip')||null});
        if(u.role==='GROOMER') {
            const exp=document.getElementById('groomerExperience').value;
            await api.put('/users/me/groomer-details',{
                organization:value('groomerOrganization')||null,
                qualification:value('groomerQualification')||null,
                specialty:value('groomerSpecialty')||null,
                experienceYears:exp===''?null:Number(exp)
            });
        }
        const image=document.getElementById('profileImage')?.files?.[0];
        if(image) await api.uploadMyProfileImage(image);
        showToast(image?'Profile and photo updated.':'Profile updated.','success');
        document.getElementById('accountDetails').hidden=false;
        cancelProfileEdit();
        bootstrap.Modal.getInstance(document.getElementById('accountModal'))?.hide();
        api.user={...api.user,firstName:value('profileFirstName'),lastName:value('profileLastName')};
        window.accountUser={...window.accountUser,...api.user};
        authStorage.set('pawcare_user',JSON.stringify(api.user));
        document.querySelectorAll('#userMenu .user-name').forEach(el=>el.textContent=[api.user.firstName,api.user.lastName].filter(Boolean).join(' '));
        document.querySelectorAll('#ownerName,#vetName,#gName').forEach(el=>el.textContent=api.user.firstName||'');
        if(typeof loadGroomerProfile==='function') loadGroomerProfile();
    }catch(e){document.getElementById('profileResult').textContent=e.message;}
    finally{btn.disabled=false;}
}
