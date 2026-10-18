function resetProductEditor() {
    editingProductId=null;
    document.getElementById('productEditorTitle').textContent='Add New Product';
    ['pName','pDesc','pPrice','pSale','pStock','pSku','pBrand','pWeight','pImage'].forEach(id=>document.getElementById(id).value='');
    document.getElementById('pReorder').value=0;
    document.getElementById('pFeatured').checked=false;document.getElementById('pActive').checked=true;
    document.getElementById('pPreview').hidden=true;
}
async function editProduct(id) {
    try {
        const p=await api.getProduct(id);await showSection('addProduct');editingProductId=id;
        document.getElementById('productEditorTitle').textContent='Edit Product #'+id;
        const fields={pName:p.name,pDesc:p.description,pPrice:p.price,pSale:p.salePrice,pStock:p.stockQuantity,pReorder:p.reorderLevel,pSku:p.sku,pCatId:p.categoryId,pBrand:p.brand,pWeight:p.weight};
        const category=document.getElementById('pCatId');
        if(![...category.options].some(o=>o.value===String(p.categoryId)))category.add(new Option(p.categoryName,p.categoryId));
        Object.entries(fields).forEach(([key,val])=>document.getElementById(key).value=val??'');
        document.getElementById('pFeatured').checked=p.isFeatured;document.getElementById('pActive').checked=p.isActive;
        const preview=document.getElementById('pPreview');preview.hidden=!p.image;if(p.image)preview.src=p.image;
    }catch(e){showToast(e.message,'error');}
}
async function saveCategory() {
    const btn=document.getElementById('saveCategoryBtn'),name=value('categoryName');
    if(btn.disabled)return;
    if(!name){showToast('Enter a category name.','error');document.getElementById('categoryName').focus();return;}
    if(!validateFields(['categoryName','categoryDescription']))return;
    btn.disabled=true;
    try {
        const category=await api.post('/categories',{name,description:value('categoryDescription')});
        setOptions('pCatId',await api.getCategories(),'Choose category');document.getElementById('pCatId').value=category.id;
        document.getElementById('categoryName').value='';document.getElementById('categoryDescription').value='';
        showToast('Category created and selected.','success');
    }catch(e){showToast(e.message,'error');}finally{btn.disabled=false;}
}
if(api.getRole()==='ADMIN') {
    document.querySelector('.sidebar-brand small').textContent='Admin Panel';
    document.querySelector('.dashboard-header h4').textContent='Product Management';
    document.querySelector('.sidebar-nav').insertAdjacentHTML('afterbegin','<div class="nav-item"><a class="nav-link" href="/pages/dashboard/admin.html"><i class="bi bi-arrow-left"></i>Admin Dashboard</a></div>');
}
const productToEdit=new URLSearchParams(location.search).get('edit');
if(productToEdit && /^[0-9]+$/.test(productToEdit))editProduct(Number(productToEdit));
else if(location.hash==='#addProduct')showSection('addProduct');
