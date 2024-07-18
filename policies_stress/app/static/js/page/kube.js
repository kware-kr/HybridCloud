document.addEventListener('DOMContentLoaded', () => {
    const searchForm = document.getElementById('search-form')
    const btnOptReset = document.getElementById('btn-opt-reset')
    const btnOptHelp = document.getElementById('btn-opt-help')
    const configList = document.getElementById('config-list')
    const configView = document.getElementById('config-view')
    const configForm = document.getElementById('config-form')
    const configFile = document.getElementById('config-file')
    const modifyFile = document.getElementById('modify-file')
    const cancelFile = document.getElementById('cancel-file')
    const configFilePreview = document.getElementById('config-file-preview')

    const itemClassList = [
        'config-item', 'border-2', 'border-cocoa-200', 'rounded', 'flex', 'justify-between',
        'select-none', 'cursor-pointer', 'hover:bg-cocoa-400', 'px-4', 'py-2'
    ]

    const search = (value) => {
        const items = configList.querySelectorAll('div.config-item')
        if ( value === '' ) {
            searchForm.search.value = value
            items.forEach(item => item.classList.remove('hidden'))
            return
        }

        items.forEach(item => {
            item.dataset.name.includes(value) || item.dataset.url.includes(value)
                ? item.classList.remove('hidden')
                : item.classList.add('hidden')
        })
    }

    searchForm.addEventListener('submit', e => {
        e.preventDefault()
        search(e.target.search.value)
    })
    btnOptReset.addEventListener('click', () => search(''))

    btnOptHelp.addEventListener('click', () => {
        const link = Helpers.linkHtml('수집 설정', '/settings/apis/view')
        const green = (text) => `<span class="text-green-600">${text}</span>`
        const html = Helpers.listHtml(`${link}의 ${green('프로메테우스 URL')}과 매칭되기 때문에 ${green('먼저 URL을 등록')}하셔야 이 곳에도 표시됩니다.`)
        Modal.short(html, '도움말')
    })

    const disableFile = (disable, showCancel = false) => {
        if ( disable ) {
            configFile.classList.add('hidden')
            modifyFile.classList.remove('hidden')
            configFilePreview.classList.remove('hidden')
            cancelFile.classList.add('hidden')
        }
        else {
            configFile.classList.remove('hidden')
            modifyFile.classList.add('hidden')
            configFilePreview.classList.add('hidden')
            showCancel && cancelFile.classList.remove('hidden')
        }
    }

    const handleClick = async (item, config) => {
        configForm.reset()
        configForm.displayName.value = config.display_name
        configForm.prometheusUrl.value = config.prometheus_url
        configFilePreview.textContent = config.config

        disableFile(config.config)

        const didOpen = () => configForm.displayName.focus()

        const isConfirmed = await Modal.fire(
            config.display_name, configView, true,
            '수정', '취소', didOpen)
        if ( isConfirmed ) {
            const formData = new FormData(configForm)
            formData.append('prometheusUrl', config.prometheus_url)

            await new Http()
                .successMessage("수정되었습니다.")
                .onSuccess((result) => {
                    configForm.displayName.value = result.display_name
                    configForm.prometheusUrl.value = result.prometheus_url
                    disableFile(result.config)
                    item.querySelector('span').textContent = result.display_name
                })
                .post("/kube/", formData)
        }
    }

    configForm.addEventListener('submit', (e) => {
        e.preventDefault()
        document.querySelector('.swal2-confirm').click()
    })

    modifyFile.addEventListener('click', () => {
        disableFile(false, !!configFilePreview.textContent)
    })
    cancelFile.addEventListener('click', () => {
        configFile.value = ''
        disableFile(true)
    })

    new Http()
        .disableAllMessage()
        .get("/kube/")
        .then(configs => {
            for ( const config of configs ) {
                const item = document.createElement('div')
                item.classList.add(...itemClassList)
                item.dataset.url = config.prometheus_url
                item.dataset.name = config.display_name
                item.addEventListener('click', () => handleClick(item, config))

                const name = document.createElement('span')
                name.textContent = config.display_name
                item.appendChild(name)

                const date = document.createElement('span')
                date.textContent = Helpers.date2string(config.date_created)
                item.appendChild(date)

                configList.appendChild(item)
            }
        })
})