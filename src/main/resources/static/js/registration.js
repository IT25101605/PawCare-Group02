const registrationRoles={
    PET_OWNER:{title:'Pet owner details',help:'Use your home address for your pet care account.',fields:[]},
    VETERINARIAN:{title:'Veterinarian credentials',help:'Your professional application will be reviewed by an administrator.',fields:[
        ['organization','Clinic / hospital name','text',150],['licenseNumber','Veterinary registration / license number','text',100],
        ['qualification','Veterinary qualification','text',200],['experienceYears','Years of experience','number',60]]},
    GROOMER:{title:'Groomer details',help:'Tell us about your grooming work. Administrator approval is required.',fields:[
        ['organization','Salon / business name','text',150],['qualification','Grooming qualification / certificate','text',200],['specialty','Grooming specialty / services','text',150],['experienceYears','Years of experience','number',60]]},
    STORE_MANAGER:{title:'Store details',help:'Register your store for administrator review.',fields:[
        ['organization','Store / business name','text',150],['businessRegistration','Business registration number','text',100]]},
    SERVICE_STAFF:{title:'Service staff details',help:'Use the employee details provided by your organization. Administrator approval is required.',fields:[
        ['organization','Organization name','text',150],['employeeId','Employee ID','text',100],['department','Department','text',100]]}
};
function renderRoleFields(role) {
    const config=registrationRoles[role];
    document.getElementById('roleFields').innerHTML='<h6>'+config.title+'</h6><p class="text-muted small">'+config.help+'</p>'+
        config.fields.map(([id,label,type,max])=>'<div class="mb-3"><label for="'+id+'" class="form-label">'+label+'</label><input id="'+id+'" name="'+id+'" type="'+type+'" required class="form-control form-control-pawcare" '+(type==='number'?'min="0" max="60" step="1"':'maxlength="'+max+'"')+'></div>').join('');
    document.getElementById('regBtn').textContent=role==='PET_OWNER'?'Create Account':'Submit Application';
    if(role==='GROOMER') {document.getElementById('qualification').required=false;GroomerFields.attach({organization:'organization',qualification:'qualification',specialty:'specialty'});}
    document.querySelectorAll('#roleFields input').forEach(configureField);
}
selectRole=function(el,role) {
    document.querySelectorAll('.role-option').forEach(o=>{o.classList.remove('selected');o.setAttribute('aria-pressed','false');});
    el.classList.add('selected');el.setAttribute('aria-pressed','true');
    document.getElementById('role').value=role;renderRoleFields(role);
};
handleRegister=async function(event) {
    event.preventDefault();
    const form=document.getElementById('regForm'),role=value('role'),btn=document.getElementById('regBtn');
    if(btn.disabled || !validateFields([...form.elements].map(e=>e.id).filter(Boolean)))return;
    const password=document.getElementById('password').value;
    if(password!==document.getElementById('confirmPassword').value) {showToast('Passwords do not match','error');return;}
    const data={firstName:value('firstName'),lastName:value('lastName'),email:value('email').toLowerCase(),phone:value('phone'),
        password,role,address:value('address'),city:value('city'),zipCode:value('zipCode')};
    for(const [id] of registrationRoles[role].fields) data[id]=id==='experienceYears'?Number(value(id)):value(id);
    const old=btn.textContent;btn.disabled=true;btn.textContent='Submitting...';
    try {
        const result=await api.register(data);
        if(result.token) { api.setAuth(result);window.location.href=GroomingBooking.afterAuth(result.role||'PET_OWNER','/pages/dashboard/owner.html'); }
        else {
            form.hidden=true;
            const notice=document.createElement('div');notice.className='alert alert-success';notice.setAttribute('role','status');
            notice.textContent=result.message;form.after(notice);
        }
    } catch(e) {
        let notice=document.getElementById('registrationError');
        if(!notice){notice=document.createElement('div');notice.id='registrationError';notice.className='alert alert-danger';notice.setAttribute('role','alert');btn.before(notice);}
        notice.textContent=e.message;
        const field=Object.keys(e.fields||{}).map(id=>document.getElementById(id)).find(Boolean);
        if(field){field.setCustomValidity(e.fields[field.id]);field.reportValidity();field.focus();}
    }
    finally {btn.disabled=false;btn.textContent=old;}
};
document.getElementById('password').minLength=8;
document.getElementById('password').maxLength=72;
document.getElementById('password').pattern='(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S+';
document.getElementById('password').title='8–72 characters, with uppercase, lowercase, a number and a symbol; no spaces';
document.querySelectorAll('.role-option').forEach(el=>{
    el.tabIndex=0;el.setAttribute('role','button');el.setAttribute('aria-pressed',el.classList.contains('selected'));
});
renderRoleFields(value('role'));
document.getElementById('phone').placeholder='0712345678';
