document.addEventListener('DOMContentLoaded', () => {
    const FONT_FAMILY = 'NanumSquareNeo'

    const container = document.getElementById('container')
    const dropdown = document.getElementById('instances')
    const canvas = document.getElementById('canvas')
    const canvas2 = document.getElementById('canvas2')
    const btnContainer = document.getElementById('btn-container')
    const btnRecent = document.getElementById('btn-recent')
    const btnPerpod = document.getElementById('btn-perpod')
    const labelHintContainer = document.getElementById('label-hint')
    const labelHint = labelHintContainer.querySelector('p')
    const btnLabelHint = labelHintContainer.querySelector('button')
    const btnRefresh = document.getElementById('btn-refresh')
    const btnTest = document.getElementById('btn-stress')
    const lineLegendContainer = document.getElementById('line-legends-container')
    const pieLegendContainer = document.querySelectorAll('.pie-legends-container')

    let chart = new Chart(canvas, { type: 'line' })
    let chart2 = new Chart(canvas2, { type: 'pie' })
    let resourceDatas
    let isLine = false

    const fixContainerSize = (height = 500) => {
        const tempHeight = `${height + 1}px`
        const cHeight = container.style.height != tempHeight ? tempHeight : `${height}px`
        container.style.height = cHeight
    }

    const refresh = async () => {
        const initialized = await initRecent()
        if ( initialized ) {
            if ( isLine ) {
                await drawLine()
            }
            else {
                await loadResource()
                await drawPie()
            }
        }
    }

    const switchGraph = (toLine = true) => {
        isLine = toLine
        labelHintContainer.classList.add('hidden')
        btnRefresh.classList.remove('hidden')
        dropdown.classList.remove('hidden')
        if ( toLine ) {
            btnContainer.classList.remove('md:mt-3')
            btnRecent.setAttribute('disabled', 'disabled')
            btnPerpod.removeAttribute('disabled')
            canvas.classList.remove('m-auto', 'mt-14')
            canvas.classList.add('mt-10')
            canvas.parentElement.classList.remove('!w-1/2')
            canvas.parentElement.classList.add('!w-full')
            canvas2.parentElement.classList.remove('!w-1/2')
            canvas2.parentElement.classList.add('!w-0')
            lineLegendContainer.classList.remove('hidden')
            for ( const container of pieLegendContainer )
                container.classList.add('hidden')
        }
        else {
            btnContainer.classList.add('md:mt-3')
            btnPerpod.setAttribute('disabled', 'disabled')
            btnRecent.removeAttribute('disabled')
            canvas.classList.remove('mt-10')
            canvas.classList.add('m-auto', 'mt-14')
            canvas.parentElement.classList.remove('!w-full')
            canvas.parentElement.classList.add('!w-1/2')
            canvas2.parentElement.classList.remove('!w-0')
            canvas2.parentElement.classList.add('!w-1/2')
            for ( const container of pieLegendContainer )
                container.classList.remove('hidden')
            lineLegendContainer.classList.add('hidden')
        }
    }

    // https://github.com/chartjs/Chart.js/blob/master/docs/samples/legend/html.md
    const getOrCreateLegendList = (chart, id, scrollable) => {
        const legendContainer = document.getElementById(id)
        let listContainer = legendContainer.querySelector('ul')

        if ( !listContainer ) {
            listContainer = document.createElement('ul')

            const flexDirection = scrollable ? ['flex-col'] : ['sm:flex-row', 'flex-col']
            listContainer.classList.add('flex', 'm-0', 'p-0', ...flexDirection)

            legendContainer.appendChild(listContainer)
        }

        return listContainer
    }
    const htmlLegendPlugin = {
        id: 'htmlLegend',
        afterUpdate(chart, args, options) {
            if ( !options.containerID ) {
                return
            }
            const ul = getOrCreateLegendList(chart, options.containerID, options.scrollable)

            // Remove old legend items
            while ( ul.firstChild ) {
                ul.firstChild.remove()
            }

            // Reuse the built-in legendItems generator
            const items = chart.options.plugins.legend.labels.generateLabels(chart)

            items.forEach(item => {
                const li = document.createElement('li')
                li.style.alignItems = 'center'
                li.style.cursor = 'pointer'
                li.style.display = 'flex'
                li.style.flexDirection = 'row'
                li.style.marginLeft = '10px'

                li.onclick = () => {
                    const { type } = chart.config
                    if ( type === 'pie' || type === 'doughnut' ) {
                        // Pie and doughnut charts only have a single dataset and visibility is per item
                        chart.toggleDataVisibility(item.index)
                    }
                    else {
                        chart.setDatasetVisibility(item.datasetIndex, !chart.isDatasetVisible(item.datasetIndex))
                    }
                    chart.update()
                }

                // Color box
                const boxSpan = document.createElement('span')
                boxSpan.style.background = item.fillStyle
                boxSpan.style.borderColor = item.strokeStyle
                boxSpan.style.borderWidth = item.lineWidth + 'px'
                boxSpan.style.display = 'inline-block'
                boxSpan.style.height = '20px'
                boxSpan.style.marginRight = '10px'
                boxSpan.style.width = '20px'

                // Text
                const textContainer = document.createElement('p')
                // textContainer.style.color = item.fontColor
                textContainer.style.margin = 0
                textContainer.style.padding = 0
                textContainer.style.textDecoration = item.hidden ? 'line-through' : ''

                const text = document.createTextNode(item.text)
                textContainer.appendChild(text)

                li.appendChild(boxSpan)
                li.appendChild(textContainer)
                ul.appendChild(li)
            })
        }
    }

    const _drawLine = async (node, prometheus_url, title = '') => {
        try {
            chart != null && chart.destroy()
            chart = new Chart(canvas, {
                type: 'line',
                plugins: [htmlLegendPlugin],
            })
            loading.classList.remove('hidden')

            const metrics_data = await new Http()
                .disableAllMessage()
                .get('/metrics/monitoring', {
                    node, prometheus_url
                })

            const colors = Helpers.randColors()

            const labels = metrics_data.map(x => {
                const date = x['date_created']
                return x['is_predict'] ? `[예측] ${Helpers.date2string(date, 'hh24:mi:ss')}` : Helpers.date2string(date)
            })

            const label2kor = {
                'cpu_usage': 'CPU 사용량', 'dsk_usage': 'Disk 사용량', 'mem_usage': 'Memory 사용량'
            }
            const dataset_labels = ['cpu_usage', 'dsk_usage', 'mem_usage']
            const datasets = dataset_labels.map(label => {
                const data = metrics_data.map(x => x[label])
                const borderColor = colors.pop()
                return {
                    label: label2kor[label],
                    data,
                    borderColor,
                    fill: false,
                    cubicInterpolationMode: 'monotone',
                    tension: 0.4
                }
            })

            chart.options = {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: title,
                        family: FONT_FAMILY,
                        font: {
                            size: 24
                        }
                    },
                    htmlLegend: {
                        containerID: 'line-legends',
                    },
                    legend: {
                        display: false,
                        position: "top",
                        align: "end",
                        labels: {
                            font: {
                                family: FONT_FAMILY,
                                size: 12,
                                weight: 'bolder',
                                lineHeight: 1.6
                            }
                        },
                    },
                    tooltip: {
                        titleFont: {
                            family: FONT_FAMILY,
                        },
                        bodyFont: {
                            family: FONT_FAMILY,
                        }
                    }
                },
                interaction: {
                    intersect: false,
                },
                scales: {
                    x: {
                        display: true,
                        title: {
                            display: true
                        },
                        ticks: {
                            font: {
                                family: FONT_FAMILY,
                            }
                        }
                    },
                    y: {
                        display: true,
                        title: {
                            display: true,
                            text: 'Percentage',
                            font: {
                                size: 14,
                                family: FONT_FAMILY,
                                weight: 'bolder',
                                lineHeight: 3
                            }
                        },
                        ticks: {
                            // FONT_COLOR,
                            callback: (v, _, _2) => `${parseFloat(v).toFixed(2)}%`,
                            font: {
                                family: FONT_FAMILY,
                                weight: 'bolder',
                            }
                        }
                    }
                }
            }

            chart.data = {
                labels, datasets
            }

            await chart.update()
        } catch (e) {
            console.error(e)
            Noti.error('오류', '잠시 후 다시 시도해주세요.')
        } finally {
            loading.classList.add('hidden')
        }
        fixContainerSize(500)
    }

    const drawLine = async () => {
        switchGraph(true)

        const option = dropdown.options[dropdown.selectedIndex]
        const node = dropdown.value
        const promUrl = option.getAttribute('data-url')

        await _drawLine(node, promUrl)
    }

    const tiny2Type = {
        'cpu': 'cpu', 'mem': 'memory', // 'dsk': 'disk'
    }

    const _drawPie = (i, type) => {
        const option = dropdown.options[dropdown.selectedIndex]
        const node = option.value
        const promUrl = option.getAttribute('data-url')

        const resDatas = resourceDatas.find(x => {
            const data = Object.values(x)[0]
            return node === data.node && promUrl === data.instance
        })

        const resData = resDatas[type]
        const label = tiny2Type[type]
        const labels = Object.keys(resData.pods)
        const data = Object.values(resData.pods)
        const total = resData.total
        const dataPercentage = data.map(x => x / total * 100)

        const colors = Helpers.randColors(data.length)
        const backgroundColor = data.map(() => colors.pop())

        const options = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false,
                    position: "top",
                    align: "end",
                    labels: {
                        font: {
                            family: FONT_FAMILY,
                            size: 12,
                            weight: 'bolder',
                            lineHeight: 1.6
                        }
                    },
                },
                tooltip: {
                    titleFont: {
                        family: FONT_FAMILY,
                    },
                    bodyFont: {
                        family: FONT_FAMILY,
                    }
                }
            },
            parsing: false,
            normalized: true,
            // animation: false,
            devicePixelRatio: 2
        }
        const datasetData = {
            backgroundColor,
            hoverOffset: 4,
            borderWidth: 2,
            borderColor: 'rgb(216,204,194)'
        }
        const chartData = {
            labels,
            datasets: [
                {
                    ...datasetData,
                    label,
                    data,
                },
                {
                    ...datasetData,
                    label: `${label}%`,
                    data: dataPercentage,
                }
            ]
        }

        if ( i == 0 ) {
            chart != null && chart.destroy()
            chart = new Chart(canvas, { type: 'pie', plugins: [htmlLegendPlugin] })
            options.plugins.htmlLegend = { containerID: 'pie-legends', scrollable: true }
            chart.options = options
            chart.data = chartData
            chart.update()
        }
        else {
            chart2 != null && chart2.destroy()
            chart2 = new Chart(canvas2, { type: 'pie', plugins: [htmlLegendPlugin] })
            options.plugins.htmlLegend = { containerID: 'pie2-legends', scrollable: true }
            chart2.options = options
            chart2.data = chartData
            chart2.update()
        }

        return [labels, data]
    }

    const drawPie = async () => {
        await loadResource()

        switchGraph(false)

        let legendSize = 0
        const types = Object.keys(tiny2Type)
        types.forEach((type, i) => {
            const [labels, _] = _drawPie(i, type)
            if ( legendSize < labels.length ) {
                legendSize = labels.length
            }
        })

        try {
            loading.classList.remove('hidden')

        } catch (e) {
            console.error(e)
            Noti.error('오류', '잠시 후 다시 시도해주세요.')
        } finally {
            loading.classList.add('hidden')
        }

        let containerSize = 400
        // if ( legendSize > 10 ) {
        //     containerSize += (legendSize - 10) / 5 * 100
        // }
        fixContainerSize(containerSize)
    }

    const initRecent = async () => {
        const instances = await new Http()
            .disableAllMessage()
            .get('/metrics/instances')

        if ( instances.length === 0 ) {
            return false
        }

        instances.sort((x, y) => Helpers.compareFn(x.prometheus_url, y.prometheus_url) || Helpers.compareFn(x.instance, y.instance))

        const selectedOption = dropdown.options[dropdown.selectedIndex]
        const url = selectedOption && selectedOption.getAttribute('data-url')
        const value = selectedOption && selectedOption.value

        dropdown.innerHTML = ''
        instances.forEach(instance => {
            const option = document.createElement('option')
            option.textContent = `${instance.instance} (${instance.prometheus_url})`
            option.value = instance.instance
            option.setAttribute('data-url', instance.prometheus_url)
            dropdown.appendChild(option)
        })

        if ( selectedOption ) {
            for ( const child of dropdown.children ) {
                if ( child.getAttribute('data-url') === url && child.value === value ) {
                    child.selected = true
                }
            }
        }

        return true
    }

    const loadResource = async () => {
        const resourceSet = await new Http()
            .disableAllMessage()
            .get('/metrics/resources')

        const resources = resourceSet.value
        const max = resourceSet.max

        const nodeGroup = Helpers.groupBy(['prom_url', 'node', 'data_type'], resources)
        const instGroup = Helpers.groupBy(['data_type', 'prom_url'], nodeGroup)

        const types = {} // types{ instances[ { node, pods[ { podName: value } ] } ] }
        instGroup.forEach(inst => {
            const dataType = inst['data_type']
            const promUrl = inst['prom_url']
            const list = []
            inst.values.forEach(v => {
                const node = v['node']
                const pods = {}
                v.values.forEach(x => pods[x['pod']] = parseFloat(x.value))
                const sum = Object.values(pods).reduce((acc, cur) => acc + cur, 0)
                const total = max.find(m => m['data_type'] === dataType && m['prom_url'] === promUrl && m['node'] === node).value
                if ( total - sum > 0 ) pods['idle or etc'] = total - sum

                const item = { node, pods, total }
                list.push(item)
            })

            if ( types[dataType] === undefined ) types[dataType] = {}
            types[dataType][promUrl] = list
        })

        resourceDatas = []

        Object.entries(types).forEach(([type, instances]) => {
            Object.keys(instances).forEach(instance => {
                Object.values(instances[instance]).forEach(nodeSet => {
                    const node = nodeSet.node
                    const total = nodeSet.total
                    const pods = nodeSet.pods

                    const resourceData = resourceDatas.find(x => {
                        const v = Object.values(x)
                        return v.length > 0 && v[0].instance === instance && v[0].node === node
                    })
                    if ( resourceData === undefined ) {
                        resourceDatas.push({ [type]: { instance, node, pods, total } })
                    }
                    else {
                        resourceData[type] = { instance, node, pods, total }
                    }
                })
            })
        })

        resourceDatas.sort((x, y) => Helpers.compareFn(x.instance, y.instance) || Helpers.compareFn(x.node, y.node))
    }

    const show = (...els) => els.forEach(el => el.classList.remove('!opacity-0'))

    const init = async () => {
        const initialized = await initRecent()
        if ( initialized ) {
            btnLabelHint.classList.add('hidden')
            btnContainer.classList.remove('hidden')
            show(btnRecent, btnPerpod, btnTest)
            labelHint.textContent = '아래 사용량 버튼을 클릭하시면 그래프를 표시합니다.'
            dropdown.addEventListener('change', () => refresh())
            btnRecent.addEventListener('click', () => drawLine())
            btnPerpod.addEventListener('click', () => drawPie())
            btnRefresh.addEventListener('click', () => refresh())
        }
        else {
            labelHint.textContent = '수집된 데이터가 없습니다.'
            btnLabelHint.classList.remove('hidden')
        }
        labelHintContainer.classList.remove('!opacity-0')
        return initialized
    }

    btnLabelHint.addEventListener('click', async () => {
        const initialized = await init()
        if ( !initialized ) {
            const link = Helpers.linkHtml('수집을 시작', '/settings/apis/view')
            Noti.error('수집된 데이터가 없습니다.', `먼저 ${link}해주세요.<br>시작하셨다면 15초 이내에 수집됩니다.`)
        }
        else Noti.fire('알림', '데이터를 불러왔습니다.')
    })
    init()
})