document.addEventListener("htmx:configRequest", (evt) => {
    let httpVerb = evt.detail.verb.toUpperCase();
    if (httpVerb === 'GET') return;

    evt.detail.headers['X-CSRF-Token'] = csrfToken;
});

document.addEventListener("DOMContentLoaded", () => {
    let icons = document.querySelectorAll(".site-icon")
    icons.forEach(el => {
        el.addEventListener('error', (e) => {
            e.target.style.display = 'none'
        })
    })

    // Get all "navbar-burger" elements
    const navbarBurger = document.querySelector('.navbar-burger')

    // Add a click event on each of them
    navbarBurger.addEventListener('click', () => {
        // Get the target from the "data-target" attribute
        const target = navbarBurger.dataset.target;
        const $target = document.getElementById(target);

        // Toggle the "is-active" class on both the "navbar-burger" and the "navbar-menu"
        navbarBurger.classList.toggle('is-active');
        $target.classList.toggle('is-active');
    });

    let configToggles = document.querySelectorAll('.config-toggles input[type="checkbox"]')
    configToggles.forEach(el => {
        el.addEventListener('change', (e) => {
            let target = e.target
            target.value = target.checked

            fetch('/api/user/settings', {
                method: 'put',
                body: JSON.stringify({[target.name]: target.checked}),
                headers: {"Content-type": "application/json"}
            })
        })
    })

})