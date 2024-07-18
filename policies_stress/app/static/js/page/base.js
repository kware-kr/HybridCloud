const loading = document.getElementById('loading')
const icoClose = document.getElementById('ico-close')
const notifyContainer = document.getElementById('notify-container')

class Modal {
    static CONFIRM_BUTTON_TEXT = "확인"
    static CANCEL_BUTTON_TEXT = "취소"

    static async fire(title, html, showCancelButton = false,
                      confirmButtonText = this.CONFIRM_BUTTON_TEXT,
                      cancelButtonText = this.CANCEL_BUTTON_TEXT,
                      didOpen = undefined) {
        const { isConfirmed } = await Swal.fire({
            title,
            html,
            showCancelButton,
            confirmButtonText,
            cancelButtonText,
            didOpen,
        })
        return isConfirmed
    }

    static short(html, title = '') {
        Swal.fire({
            title,
            html,
            confirmButtonText: this.CONFIRM_BUTTON_TEXT
        })
    }
}

class Noti {
    static GREEN = 'text-green-700'
    static RED = 'text-red-700'
    static NOTIFY_DURATION = 5000
    static TRANS_DURATION = 500
    static MAX_NOTIFY_CNT = 6
    static NOTIFY_CLOSE_CLASS = ['absolute', 'top-2', 'right-2', 'cursor-pointer', 'hover:text-cocoa-500']
    static NOTIFY_ITEM_CLASS = ['card', 'opacity-0', 'transition-all', `duration-${Noti.TRANS_DURATION}`]

    static notifyTasks = []
    static removeTasks = []
    static mouseHover = false

    static setPosition(toLeft = true) {
        toLeft
            ? notifyContainer.classList.remove('right-side')
            : notifyContainer.classList.add('right-side')
    }

    static fire(title, html, duration = this.NOTIFY_DURATION) {
        const coloredTitle = `<p class="dark:drop-shadow ${this.GREEN}">${title}</p>`
        return this._notify(`${coloredTitle}<p>${html}</p>`, duration)
    }

    static error(title, html, duration = this.NOTIFY_DURATION) {
        const coloredTitle = `<p class="dark:drop-shadow ${this.RED}">${title}</p>`
        return this._notify(`${coloredTitle}<p>${html}</p>`, duration)
    }

    static _notify(html, duration = Noti.NOTIFY_DURATION) {
        if ( this.notifyTasks.length > Noti.MAX_NOTIFY_CNT - 1 ) {
            const earliest = this.notifyTasks.shift()
            this.removeNotify(earliest.item, earliest.id)
        }

        const item = document.createElement('div')
        item.classList.add(...this.NOTIFY_ITEM_CLASS)
        item.addEventListener('mouseover', () => this.mouseHover = true)
        item.addEventListener('mouseleave', () => this.mouseHover = false)

        const content = document.createElement('div')
        content.innerHTML = html

        const button = document.createElement('div')
        button.classList.add(...this.NOTIFY_CLOSE_CLASS)
        button.appendChild(icoClose.cloneNode(true))
        button.addEventListener('click', () => this.removeNotify(item, null, true))

        item.appendChild(content)
        item.appendChild(button)

        notifyContainer.appendChild(item)
        setTimeout(() => item.classList.remove('opacity-0'), 100)

        const id = duration > 0 ? setTimeout(() => this.removeNotify(item), duration) : null
        const task = { item, id }
        this.notifyTasks.push(task)

        this._translateNotifies()

        return task
    }

    static _translateNotifies() {
        let accHeight = 0
        this.notifyTasks.forEach((task) => {
            task.item.style.transform = `translateY(-${accHeight}px)`
            accHeight += task.item.offsetHeight + 10
        })
    }

    static clearAll(force = false) {
        this.notifyTasks.reverse().forEach(task => this.removeNotify(task.item, task.id, force))
    }

    static removeShift() {
        const earliest = this.notifyTasks.shift()
        return this.removeNotify(earliest.item, earliest.id)
    }

    static async removeNotify(item, id = null, force = false) {
        if ( !force ) {
            while ( this.mouseHover ) {
                await Helpers.delay()
            }
        }

        if ( id === null ) {
            this.notifyTasks = this.notifyTasks.filter(task => {
                const eq = task.item === item
                eq && clearTimeout(task.id)
                return !eq
            })
            this._translateNotifies()
        }
        else clearTimeout(id)

        item.classList.add('opacity-0')

        const removeId = setTimeout(() => {
            item.remove()
            this.removeTasks = this.removeTasks.filter(x => x !== removeId)
        }, Noti.TRANS_DURATION)
        this.removeTasks.push(removeId)
    }
}