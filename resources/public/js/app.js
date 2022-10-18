document.addEventListener("htmx:configRequest", (evt) => {
    let httpVerb = evt.detail.verb.toUpperCase();
    if (httpVerb === 'GET') return;

    evt.detail.headers['X-CSRF-Token'] = csrfToken;
});

document.addEventListener("DOMContentLoaded", (e) => {
    let icons = document.querySelectorAll(".site-icon")
    icons.forEach(el => {
        el.addEventListener('error', (e) => {
            e.target.style.display = 'none'
        })
    })
})