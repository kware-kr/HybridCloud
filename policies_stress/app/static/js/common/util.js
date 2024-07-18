class Helpers {
    static lpad(v, l) {
        const len = l - v.toString().length
        let s = '', i = 0
        while ( i++ < len ) s += '0'
        return s + v.toString()
    }

    static date2string(date, f = 'yyyy-mm-dd hh24:mi:ss') {
        if ( date instanceof Date ) {
        }
        else if ( typeof date === 'string' ) {
            if ( date.length === 0 ) return ''
            date = new Date(date)
        }
        else if ( typeof date === 'number' ) {
            date = new Date(date)
        }
        else return ''

        return f.replace(/(yyyy|yy|mm|dd|E|hh24|hh|mi|ss|a\/p|m10i)/gi, function ($1) {
            switch ( $1 ) {
                case 'yyyy':
                    return date.getFullYear().toString();
                case 'yy':
                    return Helpers.lpad(date.getFullYear() % 1000, 2);
                case 'mm':
                    return Helpers.lpad(date.getMonth() + 1, 2);
                case 'dd':
                    return Helpers.lpad(date.getDate(), 2);
                case 'hh24':
                    return Helpers.lpad(date.getHours(), 2);
                case 'hh':
                    return Helpers.lpad(date.getHours() > 12 ? date.getHours() - 12 : date.getHours(), 2);
                case 'mi':
                    return Helpers.lpad(date.getMinutes(), 2);
                case 'ss':
                    return Helpers.lpad(date.getSeconds(), 2);
                case 'a/p':
                    return date.getHours() < 12 ? 'AM' : 'PM';
                case 'm10i':
                    return Helpers.lpad((Math.floor(date.getMinutes() / 10) * 10).toString(), 2);
                default:
                    return $1;
            }
        })
    }

    /**
     * ex) instances.sort((x, y) => Helpers.compareFn(x.prometheus_url, y.prometheus_url) || Helpers.compareFn(x.instance, y.instance))
     * @param x
     * @param y
     * @returns {number}
     */
    static compareFn(x, y) {
        return x == y ? 0 : x > y ? 1 : -1
    }

    /**
     * @param targets
     * @param src
     * @returns {*[]}
     */
    static groupBy(targets = [], src = []) {
        const sepa = '!@!@'
        const names = src.map(v => targets.map(k => v[k]).join(sepa))
        const groupNames = new Set(names)
        const groups = []

        Array.from(groupNames).forEach(groupName => {
            const group = {}
            const targetValues = groupName.split(sepa)
            for ( let i = 0; i < targets.length; i++ )
                group[targets[i]] = targetValues[i]
            groups.push(group)
        })

        groups.forEach(group => {
            const values = src.filter(v => {
                const corrects = targets.reduce((acc, cur) => {
                    v[cur] == group[cur] && acc.push(cur)
                    return acc
                }, [])
                return targets.length == corrects.length
            })
            group.values = values
        })

        return groups
    }

    static COLORS = [
        'rgb(255, 99, 132)',
        'rgb(255, 159, 64)',
        'rgb(255, 205, 86)',
        'rgb(75, 192, 192)',
        'rgb(54, 162, 235)',
        'rgb(153, 102, 255)',
        'rgb(201, 203, 207)'
    ]

    static randColor = () => {
        const randomInt = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min
        const h = randomInt(0, 360);
        const s = randomInt(40, 60);
        const l = randomInt(40, 60);
        return `hsl(${h},${s}%,${l}%)`
    }

    static randColors(size = this.COLORS.length) {
        if ( this.COLORS.length < size ) {
            const set = new Set()
            while ( set.size < size ) {
                set.add(this.randColor())
            }
            return Array.from(set)
        }
        return [...this.COLORS]
    }

    static linkHtml(html, href, withNewTab = false, id = null) {
        const target = withNewTab ? '_blank' : '_self'
        return `<a target="${target}" class="link" href="${href}" ${id && `id=${id}`}>${html}</a>`
    }

    static delay(milliSeconds = 1000) {
        return new Promise(resolve => setTimeout(resolve, milliSeconds))
    }

    static listHtml(...items) {
        const html = items.map(item => `<li>${item}</li>`).join('')
        return `<ul class="list-decimal text-left grid gap-1 mx-3">${html}</ul>`
    }
}
