document.addEventListener("htmx:configRequest", (evt) => {
    let httpVerb = evt.detail.verb.toUpperCase();
    if (httpVerb === 'GET') return;

    evt.detail.headers['X-CSRF-Token'] = csrfToken;
});