import * as fs from '@leawind/inventory/fs'
import log from '@leawind/inventory/log'
import { Lazy } from '@leawind/inventory/lazy'

export type Lang = {
  file: fs.Path
  translations: Record<string, string>
}

export type Translations = Record<string, string>

export class LangsManager {
  public constructor(private readonly dir: fs.Path) {
    this.dir = dir
  }

  public readonly listLangs = Lazy.of(() => {
    return this.dir
      .listSync()
      .toSorted((a, b) => a.nameNoExt.localeCompare(b.nameNoExt))
  }).toMethod()

  /**
   * language name ==> translations
   */
  public readonly langs = Lazy.of(() => {
    const result: Record<string, Lang> = {}
    for (const file of this.dir.listSync()) {
      const lang: Translations = JSON.parse(file.readTextSync())
      result[file.nameNoExt] = {
        file,
        translations: lang,
      }
    }
    return result
  }).toMethod()

  public readonly allKeys = Lazy.of(() => {
    const allKeys = new Set<string>()
    for (const { translations } of Object.values(this.langs())) {
      for (const key of Object.keys(translations)) {
        allKeys.add(key)
      }
    }
    return allKeys
  }).toMethod()

  /**
   * @returns Number of keys fixed
   */
  public fixLang(langName: string, placeholder: string, writeToFile: boolean = false): number {
    if (!(langName in this.langs())) {
      log.error(`Language '${langName}' not found`)
      return 0
    }
    const lang = this.langs()[langName]
    const allKeys = this.allKeys()
    let count = 0
    for (const key of allKeys) {
      if (!(key in lang.translations)) {
        lang.translations[key] = placeholder.replace(/\{key\}/g, key)
        count++
      }
    }

    this.sort(langName, false)

    if (writeToFile) {
      lang.file.writeSync(JSON.stringify(lang.translations, null, 2))
    }

    return count
  }

  /**
   * @returns Whether changed
   */
  public sort(langName: string, writeToFile: boolean = false): boolean {
    if (!(langName in this.langs())) {
      log.error(`Language '${langName}' not found`)
      return false
    }
    const lang = this.langs()[langName]
    const sortedEntries = Object.entries(lang.translations).sort((a, b) => a[0].localeCompare(b[0]))
    const sortedTranslations = Object.fromEntries(sortedEntries)
    const changed = JSON.stringify(lang.translations) !== JSON.stringify(sortedTranslations)
    if (changed) {
      lang.translations = sortedTranslations
      if (writeToFile) {
        lang.file.writeSync(JSON.stringify(lang.translations, null, 2))
      }
    }
    return changed
  }
}
