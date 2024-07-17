document.addEventListener('DOMContentLoaded', () => {
    const formProm = document.getElementById('form-prom')
    const listProm = document.getElementById('list-prom')
    const iconProm = document.querySelector('.icon-prom')

    const btnStart = document.getElementById('btn-start')
    const btnStop = document.getElementById('btn-stop')

    const createPromItem = (url) => {
        const item = document.createElement('div')
        item.classList.add('border-l-4', 'border-cocoa-500', 'py-1', 'pl-3', 'pr-1', 'flex', 'justify-between', 'items-center', 'hover:bg-cocoa-200', 'hover:dark:bg-cocoa-400')

        const addr = document.createElement('span')
        addr.textContent = url

        const remove = document.createElement('div')
        remove.appendChild(iconProm.cloneNode(true))
        remove.setAttribute('title', `"${url}"을 삭제합니다.`)
        remove.addEventListener('click', async () => {
            const isConfirmed = await Modal.fire('', '삭제하시겠습니까?', true, '삭제')
            if ( isConfirmed ) {
                await new Http()
                    .successMessage('삭제되었습니다.')
                    .onSuccess(() => {
                        item.remove()
                        listProm.children.length == 0 && listProm.classList.add('hidden')
                    })
                    .delete('/settings/apis/' + btoa(url))
            }
        })

        item.append(addr)
        item.append(remove)

        listProm.appendChild(item)
        if ( listProm.children.length > 0 ) {
            listProm.classList.remove('max-h-0')
            listProm.classList.add('max-h-screen')
        }
        else {
            listProm.classList.add('max-h-0')
            listProm.classList.remove('max-h-screen')
        }
    }

    new Http()
        .disableAllMessage()
        .get('/settings/apis')
        .then(apis => {
            for ( const api of apis ) {
                createPromItem(api.value)
            }
        })

    formProm.addEventListener('submit', e => {
        e.preventDefault()

        const url = e.target.url

        if ( !Regex.url(url.value) ) {
            Noti.error('오류', '올바른 URL이 아닙니다.').then(() => url.focus())
            return
        }
        url.value = new URL(url.value).origin

        new Http()
            .successMessage('등록되었습니다.')
            .onSuccess(() => {
                createPromItem(url.value)
                url.value = ''
            })
            .post('/settings/apis', new FormData(formProm))
    })

    const hideBtnCollectStart = () => {
        btnStart.setAttribute('disabled', 'disabled')
        btnStop.removeAttribute('disabled')
    }
    const hideBtnCollectStop = () => {
        btnStop.setAttribute('disabled', 'disabled')
        btnStart.removeAttribute('disabled')
    }

    new Http()
        .disableAllMessage()
        .get('/metrics/collect')
        .then(collecting => collecting ? hideBtnCollectStart() : hideBtnCollectStop())

    btnStart.addEventListener('click', () => {
        const link = Helpers.linkHtml('여기', '/metrics/monitoring/view')
        new Http()
            .successMessage(`수집을 시작합니다.<br>앞으로 15초 간격으로 메트릭이 수집됩니다.<br>수집된 데이터는 ${link}에서 확인하실 수 있습니다.`)
            .onSuccess(() => hideBtnCollectStart())
            .post('/metrics/collect', { start: true })
    })

    btnStop.addEventListener('click', () => {
        new Http()
            .successMessage('수집이 종료되었습니다.')
            .onSuccess(() => hideBtnCollectStop())
            .post('/metrics/collect', { start: false })
    })
})