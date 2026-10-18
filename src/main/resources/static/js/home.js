/* Live featured products, with recoverable loading, empty and offline states. */
const escapeProductText = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
async function loadFeatured() {
    const container = document.getElementById('featuredProducts');
    container.setAttribute('aria-busy', 'true');
    container.innerHTML = '<div class="loading-spinner" role="status" aria-label="Loading featured products"></div>';
    try {
        const products = await api.getFeaturedProducts();
        if (!Array.isArray(products)) throw new Error('Invalid product response');
        if (!products.length) {
            container.innerHTML = '<div class="col-12"><div class="product-error"><i class="bi bi-bag-heart"></i><h3>Good things are on their way.</h3><p>Our featured collection is being refreshed. Explore the store for more pet essentials.</p><a class="btn btn-pawcare-outline" href="/pages/store.html">Explore the store ↗</a></div></div>';
            return;
        }
        container.innerHTML = products.slice(0, 4).map(p => `<div class="col-sm-6 col-lg-3"><article class="product-card position-relative"><div class="product-img"><img src="${escapeProductText(p.image||'/images/products/bowl.svg')}" alt="${escapeProductText(p.name)}" loading="lazy"></div>${p.salePrice ? '<span class="badge-sale">A LITTLE TREAT</span>' : ''}<div class="product-body"><p class="text-muted small mb-2">${escapeProductText(p.categoryName || 'Everyday essentials')}</p><h3 class="h6 mb-3">${escapeProductText(p.name)}</h3><div class="d-flex justify-content-between align-items-center"><span class="product-price">${formatCurrency(p.salePrice || p.price)}${p.salePrice ? `<span class="original-price">${formatCurrency(p.price)}</span>` : ''}</span><button class="btn btn-sm btn-pawcare" data-product-id="${Number(p.id)}" aria-label="Add ${escapeProductText(p.name)} to cart" ${p.stockQuantity === 0 ? 'disabled' : ''}><i class="bi bi-plus-lg" aria-hidden="true"></i></button></div></div></article></div>`).join('');
    } catch (error) {
        container.innerHTML = '<div class="col-12"><div class="product-error"><i class="bi bi-bag-heart"></i><h3>A little pause in our pet store.</h3><p>We couldn’t load the collection just now. Please try again in a moment.</p><button type="button" class="btn btn-pawcare-outline" data-retry-products>Try again <i class="bi bi-arrow-clockwise ms-2"></i></button></div></div>';
    } finally { container.setAttribute('aria-busy', 'false'); }
}
document.getElementById('featuredProducts').addEventListener('click', async event => {
    if (event.target.closest('[data-retry-products]')) { loadFeatured(); return; }
    const button = event.target.closest('[data-product-id]');
    if (!button) return;
    if (!api.isLoggedIn()) { window.location.href = '/pages/login.html'; return; }
    button.disabled = true;
    try { await api.addToCart(Number(button.dataset.productId), 1); showToast('A little treat, added to your cart.', 'success'); }
    catch (error) { showToast(error.message || 'Could not add this item. Please try again.', 'error'); }
    finally { button.disabled = false; }
});
loadFeatured();
