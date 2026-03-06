import { program } from 'npm:commander@15.0.0-0'
import * as fs from 'npm:@leawind/inventory@0.18.6/fs'
import log from 'npm:@leawind/inventory@0.18.6/log'
import { LangsManager } from './lib/langs.ts'

const LANGUAGES_DIR = fs.P`common/src/main/resources/assets/minecraft/lang`

async function main() {
  const mgr = new LangsManager(LANGUAGES_DIR)

  const i18n = program
    .name('i18n')
    .description('Manage language translation files')

  const i18n_list = i18n.command('list').alias('ls')
  {
    i18n_list.command('langs')
      .description('List existing language files')
      .action(() => {
        console.log('Found languages:')
        for (const lang of mgr.listLangs()) {
          console.log('  - ' + lang.name)
        }
      })

    i18n_list.command('keys')
      .description('List existing translation keys in all language files')
      .action(() => {
        for (const key of mgr.allKeys()) {
          console.log(key)
        }
      })
  }

  i18n.command('fix [langs]')
    .description('Fix language translation files')
    .option('-p, --placeholder', 'Placeholder translation for missing keys', 'TODO {key}')
    .action((toFix: string = '', opts: { placeholder: string }) => {
      let langsToFix = toFix.split(/,\s*/)
      if (toFix.trim() == '') {
        langsToFix = mgr.listLangs().map((lang) => lang.nameNoExt)
      }

      let total = 0
      for (const lang of langsToFix) {
        const count = mgr.fixLang(lang, opts.placeholder, true)
        total += count
        if (count > 0) {
          log.info(`Fixed ${count} keys in language ${lang}`)
        }
      }
      log.info(`Total fixed keys: ${total}`)
    })

  // sort
  i18n.command('sort [langs]')
    .description('Sort language translation files')
    .action((toSort: string = '') => {
      let langsToSort = toSort.split(/,\s*/)
      if (toSort.trim() == '') {
        langsToSort = mgr.listLangs().map((lang) => lang.nameNoExt)
      }
      for (const lang of langsToSort) {
        const changed = mgr.sort(lang, true)
        log.info(`Sorted ${lang}${changed ? ' (changed)' : ' (no change)'}`)
      }
    })

  program.parse([Deno.execPath(), ...Deno.args])
}

if (import.meta.main) {
  await main()
}
