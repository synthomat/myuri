var data={u:document.location.href,t:document.title,d:document.querySelector('head>meta[name="description"]')?.content,bmv:1}; window.open('http://localhost:3000/new?d='+encodeURIComponent(JSON.stringify(data)), '', 'width=600,height=350')