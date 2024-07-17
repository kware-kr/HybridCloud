document.addEventListener('DOMContentLoaded', () => {
    const formSearch = document.getElementById('form-search')
    const btnOptReset = document.getElementById('btn-opt-reset')
    const btnOptSave = document.getElementById('btn-opt-save')
    const btnOptHelp = document.getElementById('btn-opt-help')

    const viewNode = document.getElementById('view-node')
    const formView = document.getElementById('form-view')
    const sortMenu = document.getElementById('sort-menu')

    const listContents = document.querySelectorAll('.list-content')
    const listContentMap = {}
    const listItems = []

    sortMenu.oncontextmenu = () => {
        return false
    }

    for ( const [i, listContent] of listContents.entries() ) {
        listContentMap[i] = listContent
        new Sortable(listContent, {
            group: 'nested',
            multiDrag: true,
            selectedClass: 'selected',
            fallbackTolerance: 3,
            animation: 150,
            fallbackOnBody: true,
            swapThreshold: 0.65,
            onEnd: (e) => {
                const to = Object.keys(listContentMap).find(k => listContentMap[k] === e.to) - 2
                e.item.dataset.sortOrder = `${to}`
                window.onbeforeunload = () => false
            }
        })
    }

    const onItemClick = async (item) => {
        formView.reset()
        formView.display.value = item.dataset.display_name
        formView.instance.value = item.dataset.instance_name
        formView.url.value = item.dataset.prometheus_url
        formView.enabled.checked = item.dataset.enabled === 'true'

        const didOpen = () => formView.display.focus()

        const isConfirmed = await Modal.fire(
            item.dataset.instance_name, viewNode,
            true, '수정', '취소', didOpen)
        if ( isConfirmed ) {
            item.dataset.enabled = formView.enabled.checked.toString()
            item.dataset.display_name = formView.display.value
            await new Http()
                .successMessage('수정되었습니다.')
                .onSuccess(() => item.textContent = item.dataset.display_name)
                .put('/instances', item.dataset)
        }
    }

    formView.addEventListener('submit', e => {
        e.preventDefault()
        document.querySelector('.swal2-confirm').click()
    })

    const search = (value) => {
        if ( value === '' ) {
            formSearch.search.value = value
            listItems.forEach(item => item.classList.remove('unfocused'))
            return
        }

        const focused = []
        const unfocused = []
        listItems.filter(item =>
            item.dataset.display_name.includes(value) ||
            item.dataset.instance_name.includes(value) ||
            item.dataset.prometheus_url.includes(value)
                ? focused.push(item)
                : unfocused.push(item))

        focused.forEach(item => item.classList.remove('unfocused'))
        unfocused.forEach(item => item.classList.add('unfocused'))
    }

    formSearch.addEventListener('submit', e => {
        e.preventDefault()
        search(e.target.search.value)
    })
    btnOptReset.addEventListener('click', () => search(''))
    btnOptSave.addEventListener('click', () => {
        const datas = listItems.map(item => ({ ...item.dataset }))
        new Http()
            .successMessage('반영되었습니다.')
            .onSuccess(() => window.onbeforeunload = null)
            .errorMessage('우선순위를 변경하는 도중 오류가 발생했습니다.')
            .put('/instances/order', datas)
    })
    btnOptHelp.addEventListener('click', () => {
        const green = (text) => `<span class="text-green-600">${text}</span>`
        const html = Helpers.listHtml(
            `${green('항목을 우클릭')}해 일부 ${green('정보를 수정')}하실 수 있습니다.`,
            `${green('동일한 우선순위')}에 있는 항목들은 ${green('동시에 이동')}시키실 수 있습니다.`,
            `항목 이동을 통한 ${green('우선순위 변경은 반영 버튼')}을 클릭하셔야 ${green('저장')}됩니다.`)
        Modal.short(html, '도움말')
    })

    new Http()
        .disableAllMessage()
        .get('/instances')
        .then(instances => {
            instances = instances.sort((x, y) => Helpers.compareFn(x.sort_order, y.sort_order))
            instances.forEach(instance => {
                const item = document.createElement('div')
                Object.entries(instance).forEach(([k, v]) => item.dataset[k] = v.toString())
                item.textContent = instance.display_name || `${instance.instance_name} (${instance.prometheus_url})`
                item.addEventListener('contextmenu', () => onItemClick(item))
                listItems.push(item)

                const list = listContents.item(instance.sort_order ? parseInt(instance.sort_order) + 2 : 2)
                list.appendChild(item)
            })
        })
})