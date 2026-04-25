/**
 * 1. 使用 Gradle 任务 `fabric:runClient` 运行客户端，手动保存结构
 * 2. 使用此脚本将结构同步到用于 gametest 的结构目录中
 *
 * @module gametest
 */

import { program } from 'commander'
import * as fs from '@leawind/inventory/fs'
import log from '@leawind/inventory/log'
import { FABRIC_GAMETEST_STRUCTURE_DIR, FABRIC_GAMETEST_STRUCTURE_SAVED_DIR } from './lib/constants.ts'

async function main() {
  const cli = program
    .name('gametest')
    .description('Manage gametest structure files')

  cli.command('list').alias('ls').action(() => {
    const saved = FABRIC_GAMETEST_STRUCTURE_SAVED_DIR.listSync()
    log.info(`Saved files: ${saved.map((f) => f.nameNoExt).join(', ')}`)
  })

  cli.command('sync')
    .description('Sync structure files to gametest structure directory')
    .action(() => {
      for (const file of FABRIC_GAMETEST_STRUCTURE_SAVED_DIR.listSync()) {
        const targetFile = FABRIC_GAMETEST_STRUCTURE_DIR.join(file.name)
        fs.copyFileSync(file, targetFile)
        log.info(`Synced ${file.nameNoExt}`)
      }
    })

  program.parse([Deno.execPath(), ...Deno.args])
}

if (import.meta.main) {
  await main()
}
