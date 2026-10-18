async function deleteManagedProduct(id,button,reload) {
    if(button.disabled)return;
    let password=null;if(api.getRole()==='STORE_MANAGER'){password=await requestPasswordConfirmation('Confirm Product Deletion');if(!password)return;}else if(!await pawConfirm('Remove product #'+id+' from the store? Previous orders and reviews will remain available.',{title:'Delete product?',type:'danger',confirmText:'Delete Product'}))return;
    button.disabled=true;
    try{await (password?api.deleteConfirmed('/products/'+id,password):api.delete('/products/'+id));await reload();showToast('Product removed from the store. Order history is preserved.','success');}
    catch(e){showToast(e.message,'error');}
    finally{button.disabled=false;}
}
