document.addEventListener('DOMContentLoaded', () => {
    const formDb = document.getElementById('form-db')
    const btnTest = document.getElementById('btn-stress')

    const paraAlert = document.createElement('p')
    paraAlert.textContent = '필수 정보입니다.'
    paraAlert.classList.add('text-red-500', 'dark:text-red-700')

    let testedValues = null

    new Http()
        .disableAllMessage()
        .get('/settings/db')
        .then(info => {
            for ( let key in info ) {
                if ( info[key] != null ) {
                    const input = formDb.querySelector(`input[name=${key}]`)
                    input.value = info[key]
                }
            }
        })

    const emptyCheck = (formData, ...names) => {
        paraAlert.parentElement && paraAlert.parentElement.removeChild(paraAlert)

        const emptyDataName = names.find(name => {
            const value = formData.get(name) ?? ''
            return value.trim().length === 0
        })
        const emptyDataExists = emptyDataName !== undefined

        if ( emptyDataExists ) {
            const emptyInput = formDb.querySelector(`input[name=${emptyDataName}]`)
            emptyInput.focus()
            emptyInput.parentElement.append(paraAlert)
        }

        return !emptyDataExists
    }

    const testCheck = (formData) => {
        if ( testedValues === null ) {
            Noti.fire('알림', '테스트를 먼저 진행해주세요.')
            return false
        }

        for ( let key in testedValues ) {
            const valueNotMatched = testedValues[key] !== formData.get(key)
            if ( valueNotMatched ) {
                Noti.fire('알림', '변경된 값이 있습니다.<br>테스트를 진행해주세요.')
                return false
            }
        }

        return true
    }


    const handleSubmit = async (e) => {
        e.preventDefault()

        const formData = new FormData(formDb)
        if ( !testCheck(formData) ) return

        await new Http()
            .successMessage('저장 완료')
            .post('/settings/db', testedValues)
    }

    const handleTest = async (e) => {
        e.preventDefault()

        const formData = new FormData(formDb)

        const isEmpty = !emptyCheck(formData, ...formData.keys())
        if ( isEmpty ) return

        const values = {}
        const keys = [...formData.keys()]
        keys.forEach(key => values[key] = formData.get(key))

        await new Http()
            .errorMessage('연결에 실패했습니다')
            .successMessage('연결 성공')
            .onSuccess(() => testedValues = values)
            .post('/settings/db/test', values)
    }

    formDb.addEventListener('submit', handleSubmit)
    btnTest.addEventListener('click', handleTest)
})
