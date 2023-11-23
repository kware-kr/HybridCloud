document.addEventListener('DOMContentLoaded', () => {
    const btnTest = document.getElementById('btn-stress')
    const btnRefresh = document.getElementById('btn-refresh')
    const btnRecent = document.getElementById('btn-recent')
    const btnPerPod = document.getElementById('btn-perpod')
    const instSelector = document.getElementById('instances')

    class StressTest {
        static DEFAULT_TEMP = { step: 0 }

        constructor() {
            this.title = 'CPU 부하 테스트'
            this.temp = StressTest.DEFAULT_TEMP
            this.data = []
            this.steps = []
            this.handleCompleted = null
        }

        _update(chunk) {
            console.log('chunk:', chunk)
            const step = this.temp.step
            if ( step === 0 ) {
                const { success, type, size, complete } = JSON.parse(chunk)
                if ( complete !== undefined ) {
                    console.log('complete')
                    this.stop()
                    return
                }

                this.temp = {
                    step: 1,
                    success,
                    type,
                    size: parseInt(size),
                    data: [],
                    len: 0,
                }

                if ( this.temp.size === 0 ) {
                    const stepCallback = this.steps[this.data.length]
                    if ( stepCallback !== undefined ) {
                        stepCallback({
                            data: this.temp.data,
                            success: this.temp.success
                        })
                    }
                    this.data.push(this.temp)
                    this.temp = StressTest.DEFAULT_TEMP
                }
            }
            else {
                let data = chunk
                if ( this.temp.type === 'int' )
                    data = parseInt(data)
                else if ( this.temp.type === 'float' )
                    data = parseFloat(data)
                else if ( this.temp.type === 'json' )
                    data = JSON.parse(data)
                else if ( this.temp.type === 'list' )
                    data = JSON.parse(data).list
                this.temp.data.push(data)
                this.temp.len += 1
                if ( this.temp.len >= this.temp.size ) {
                    const stepCallback = this.steps[this.data.length]
                    if ( stepCallback !== undefined ) {
                        stepCallback({
                            data: this.temp.data,
                            success: this.temp.success
                        })
                    }
                    this.data.push(this.temp)
                    this.temp = StressTest.DEFAULT_TEMP
                }
            }
        }

        step(callback) {
            this.steps.push(callback)
            return this
        }

        onCompleted(callback) {
            this.handleCompleted = callback
            return this
        }

        stop() {
            this.handleCompleted && this.handleCompleted()
        }

        async start(url, ask2user = true) {
            let isConfirmed = true
            if ( ask2user ) {
                isConfirmed = await Modal.fire(this.title, '시작하시겠습니까?', true, '시작')
            }
            if ( isConfirmed ) {
                btnTest.setAttribute('disabled', 'disabled')
                // Noti.clearAll()
                Noti.fire(this.title, '테스트를 시작합니다.')

                await new Http()
                    .disableSuccessMessage()
                    .stream(url, chunk => this._update(chunk))
            }
        }
    }

    const firstTest = new StressTest()

    const stressTest = async () => {
        const DEFAULT_DELAY = 16

        const notiError = (html) => {
            const content = `${html}<br><button id="close-all-notifies">전체 알림 닫기</button>`
            Noti.error('오류', content, 0)
            setTimeout(() => {
                document.getElementById('close-all-notifies').addEventListener('click', () => Noti.clearAll(true))
            }, Noti.TRANS_DURATION)
        }

        const showValidConfigs = (data) => {
            if ( data.success ) {
                const list = Helpers.listHtml(...data.data.map(d => `${d.display_name} (${d.prometheus_url})`))
                const html = `<div class="overflow-x-auto px-2">${list}</div>`
                Noti.fire('등록된 쿠버네티스 config 파일 목록', html, 0)
            }
            else {
                const link = Helpers.linkHtml('쿠버네티스 설정 메뉴', '/kube/view')
                const html = `등록된 쿠버네티스 config 파일이 없습니다.<br>${link}에서 먼저 등록하셔야 합니다.`
                notiError(html)
            }
        }

        const showCpuTestAppYaml = (data) => {
            if ( data.success ) {
                const yaml = `<textarea class="whitespace-pre-wrap text-left resize-none" rows="15" disabled>${data.data}</textarea>`
                const link = Helpers.linkHtml('deployment.yaml', '#', false, 'show-yaml')

                Noti.fire('cpu 테스트 앱 yaml', link, 0)
                setTimeout(() => {
                    document.getElementById('show-yaml').addEventListener('click', () => Modal.short(yaml))
                }, Noti.TRANS_DURATION)
            }
            else notiError(data.data)
        }

        const applyPolicy1 = (data) => {
            if ( data.success ) {
                // 자원 상태를 만족하는 노드
                const list = Helpers.listHtml(...data.data)
                const html = `<div class="overflow-x-auto px-2">${list}</div>`
                Noti.fire('자원 상태를 만족하는 노드 목록', html, 0)
            }
            else notiError(data.data)
        }

        const applyPolicy2 = (data) => {
            if ( data.success ) {
                // 노드 우선순위 적용
                Noti.fire('노드 우선순위 적용', `<div class="overflow-x-auto text-center">${data.data}</div>`, 0)
            }
            else notiError(data.data)
        }

        const deployStressApp = (data) => {
            if ( data.success ) {
                const target = data.data[0]
                Noti.fire('CPU 부하 앱 배포', `${target.node} 노드에 배포되었습니다.<br>프로메테우스 URL: ${target.prom_url}`, 0)

                btnRecent.click()
                const instances = instSelector.querySelectorAll('option')
                instances.forEach(opt => {
                    if ( target.node === opt.value && target.prom_url === opt.dataset.url ) {
                        opt.selected = true
                        const evt = new Event('change')
                        opt.parentElement.dispatchEvent(evt)
                    }
                })
                instSelector.setAttribute('disabled', 'disabled')
            }
            else notiError(data.data)
        }

        const secondTest = new StressTest()

        const checkExcluded = (data) => {
            if ( data.success ) {
                const message = data.data[0]
                Noti.fire('정책 적용 확인', message, 0)
            }
            else notiError(data.data)
        }

        const deployTestApp = (data) => {
            if ( data.success ) {
                const target = data.data[0]
                Noti.fire('CPU 테스트 앱 배포', `${target.node} 노드에 배포되었습니다.<br>프로메테우스 URL: ${target.prom_url}`, 0)
            }
            else notiError(data.data)
        }
        const deleteAllApps = (data) => {
            if ( data.success ) {
                const list = Helpers.listHtml(...data.data)
                const html = `<div class="overflow-x-auto px-2">${list}</div>`
                Noti.fire('초기화', html, 0)
            }
            else notiError(data.data)
        }

        const getPerPodUsagePct = async (prom_url, node) => {
            const resourceSet = await new Http()
                .disableAllMessage()
                .get('/metrics/resources')

            const resources = resourceSet.value
            const max = resourceSet.max

            const findNode = (x) => x['data_type'] === 'cpu' && x['prom_url'] === prom_url && x['node'] === node
            const maxNode = max.find(findNode)

            const nodeGroup = Helpers.groupBy(['prom_url', 'node', 'data_type'], resources)
            const targetNode = nodeGroup.find(findNode)

            const total = maxNode.value
            const sum = targetNode.values.reduce((acc, cur) => acc + cur.value, 0)

            return sum / total
        }

        const startSecondTest = () => {
            secondTest
                .step(checkExcluded)
                .step(deployTestApp)
                .step(deleteAllApps)
                .onCompleted(() => {
                    btnTest.removeAttribute('disabled')
                    const html = `테스트가 종료되었습니다.<br><button id="close-all-notifies">전체 알림 닫기</button>`
                    Noti.fire('완료', html, 0)
                    instSelector.removeAttribute('disabled')
                    Noti.setPosition(true)
                })
                .start('/deploy/cpu/2', false)
        }

        const startMonitoring = () => {
            const waitingId = 'waiting-message'
            const waitingHtml = `<div id="${waitingId}"></div>`
            const waitingNoti = Noti.fire('cpu 부하 체크', waitingHtml, 0)

            setTimeout(() => {
                const waitingEl = document.getElementById(waitingId)

                const cpuRecentCheck = () => {
                    let timeSpan = 0
                    const id = setInterval(async () => {
                        timeSpan += 1
                        waitingEl.textContent = `갱신까지 ${DEFAULT_DELAY - timeSpan}초`
                        if ( timeSpan >= DEFAULT_DELAY ) {
                            btnRefresh.click()
                            clearInterval(id)
                            timeSpan = 0
                            invoke()
                        }
                    }, 1000)
                }

                const cpuPerPodCheck = (prom_url, node, notify) => {
                    let timeSpan = 0
                    const id = setInterval(async () => {
                        timeSpan += 1
                        waitingEl.textContent = `갱신까지 ${DEFAULT_DELAY - timeSpan}초`
                        if ( timeSpan >= DEFAULT_DELAY ) {
                            btnRefresh.click()
                            timeSpan = 0
                            const pct = await getPerPodUsagePct(prom_url, node)
                            if ( pct > .9 ) {
                                clearInterval(id)
                                Noti.removeNotify(notify.item, notify.id)
                                startSecondTest()
                            }
                        }
                    }, 1000)
                }

                const invoke = () => {
                    new Http()
                        .disableAllMessage()
                        .get('/deploy/cpu/check')
                        .then(isOver => {
                            if ( isOver ) {
                                btnPerPod.click()
                                const selected = instSelector.options[instSelector.selectedIndex]
                                Noti.setPosition(false)
                                cpuPerPodCheck(selected.dataset.url, selected.value, waitingNoti)
                            }
                            else cpuRecentCheck()
                        })
                }

                invoke()
            }, Noti.TRANS_DURATION)
        }

        await firstTest
            .step(showValidConfigs)
            .step(showCpuTestAppYaml)
            .step(applyPolicy1)
            .step(applyPolicy2)
            .step(deployStressApp)
            .onCompleted(startMonitoring)
            .start('/deploy/cpu/1')
    }

    btnTest.addEventListener('click', () => stressTest())
})