class Http {
    constructor() {
        this._messages = {
            success: '요청 성공',
            error: '오류가 발생했습니다',
            unknown: '알 수 없는 오류'
        }
        this._callback = {
            success: null,
            error: null
        }
        this._disable = {
            success: false,
            error: false,
            unknown: false
        }
    }

    successMessage(message) {
        this._messages.success = message
        return this
    }

    errorMessage(message) {
        this._messages.error = message
        return this
    }

    unknownMessage(message) {
        this._messages.unknown = message
        return this
    }

    disableSuccessMessage() {
        this._disable.success = true
        return this
    }

    disableErrorMessage() {
        this._disable.error = true
        return this
    }

    disableUnknownMessage() {
        this._disable.unknown = true
        return this
    }

    disableAllMessage() {
        for ( const key in this._disable )
            this._disable[key] = true
        return this
    }

    onSuccess(callback) {
        this._callback.success = callback
        return this
    }

    onError(callback) {
        this._callback.error = callback
        return this
    }

    _parse_url(url, values) {
        if ( !values || Object.keys(values).length === 0 )
            return url

        const urlSearchParams = new URLSearchParams()
        for ( const key in values )
            urlSearchParams.append(key, values[key])
        return `${url}?${urlSearchParams.toString()}`
    }

    get(url, values) {
        return this._request(async () => await axios.get(this._parse_url(url, values)))
    }

    post(url, values) {
        return this._request(async () => await axios.post(url, values))
    }

    put(url, values) {
        return this._request(async () => await axios.put(url, values))
    }

    delete(url, values) {
        return this._request(async () => await axios.delete(this._parse_url(url, values)))
    }

    test(url, values) {
        return new Promise(() => {
            !this._disable.success && Noti.fire('알림', this._messages.success)
            this._callback.success && this._callback.success()
            return { url, values }
        })
    }

    async _request(_axios) {
        if ( !_axios ) return

        let requestEnd = false
        setTimeout(() => {
            !requestEnd && loading.classList.remove('hidden')
        }, 300)

        try {
            const response = await _axios()
            const isSuccess = response.status === 200

            if ( isSuccess ) {
                !this._disable.success && Noti.fire('알림', this._messages.success)
                this._callback.success && this._callback.success(response.data)
            }
            else !this._disable.unknown && Noti.fire('알림', this._messages.unknown)

            return response.data
        } catch (e) {
            console.error(e)
            if ( e.response && Object.hasOwn(e.response.data, 'detail') && e.response.data.detail )
                Noti.error('오류', e.response.data.detail)
            else if ( !this._disable.error )
                Noti.error('오류', this._messages.error)
            if ( this._callback.error )
                this._callback.error()
        } finally {
            requestEnd = true
            loading.classList.add('hidden')
        }
    }

    async stream(url, onEachChunk, loading = false) {


        let requestEnd = false
        if ( loading ) {
            setTimeout(() => {
                !requestEnd && loading.classList.remove('hidden')
            }, 300)
        }

        const response = await fetch(url)
        if ( response.ok ) {
            await this._readChunk(response, onEachChunk)
            !this._disable.success && Noti.fire('알림', this._messages.success)
            this._callback.success && this._callback.success()
        }
        else {
            const data = response.body.getReader().read()
            if ( Object.hasOwn(data, 'detail') && data.detail )
                Noti.fire('알림', data.detail)
            else if ( !this._disable.error )
                Noti.error('오류', this._messages.error)
            else if ( this._callback.error )
                this._callback.error()
        }

        if ( loading ) {
            requestEnd = true
            loading.classList.add('hidden')
        }
    }

    async _readChunk(response, onEachChunk) {
        let reading = true
        const reader = response.body.getReader()
        const decoder = new TextDecoder()

        while ( reading ) {
            const result = await reader.read()
            if ( result.value !== undefined ) {
                const chunk = decoder.decode(result.value, { stream: !result.done })
                onEachChunk(chunk)
            }
            reading = !result.done
        }
    }
}
