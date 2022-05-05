const deleteBookmark = (id) => {
    return fetch('/bookmarks/' + id, {
        method: 'DELETE',
        headers: {'X-CSRF-Token': csrfToken}
    })
}

const deleteOnClick = (el, id) => {
    el.addEventListener('click', e => {
        e.preventDefault()

        deleteBookmark(id).then(res => {
            window.location.reload()
        })
    })
}

window.onload = function () {
    document.querySelectorAll('.delete-bm[data-bm-id]')
        .forEach(el => {
            const bookmarkId = el.getAttribute('data-bm-id')
            deleteOnClick(el, bookmarkId)
        });
}