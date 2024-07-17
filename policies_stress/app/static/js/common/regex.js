class Regex {
    static url(url) {
        return /^(?:http)(?:s)?(?:\:\/\/)(?:www\.)?(?:[^ ]*)(?:\/)?(?:[^ ]*)$/gm.test(url)
    }
}