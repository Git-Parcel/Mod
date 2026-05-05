/**
 * GameTest structure file management CLI.
 *
 * Provides commands for managing Minecraft GameTest structure files used
 * by the Git Parcel mod during testing:
 *
 * - `list` / `ls` — List all saved parcels that are structure templates.
 * - `sync`        — Copy structure NBT files from saved parcel directories
 *                    into the GameTest structure resource directory so they
 *                    can be loaded by the game.
 *
 * @module gametest
 */

import {program} from 'commander'
import * as fs from '@leawind/inventory/fs'
import log from '@leawind/inventory/log'
import {FABRIC_GAMETEST_SAVED_PARCELS_DIR, FABRIC_GAMETEST_STRUCTURE_DIR} from './lib/constants.ts'

const FORMAT_NAME = 'structure_template'

const VALID_FILENAME_RE = /^[a-z0-9_\-]+(\.[a-z0-9_\-]+)*$/i

interface ParcelMeta {
  format?: { id?: string; version?: number }
  name?: string
}

function isValidFilename(name: string): boolean {
  return VALID_FILENAME_RE.test(name)
}

async function main() {
  const cli = program
    .name('gametest')
    .description('Manage gametest structure files')

  cli.command('list').alias('ls').action(() => {
    const entries = FABRIC_GAMETEST_SAVED_PARCELS_DIR.listSync()
    for (const entry of entries) {
      const parcelJson = entry.join('parcel', 'parcel.json')
      if (!parcelJson.existsSync()) {
        continue
      }
      const meta: ParcelMeta = JSON.parse(Deno.readTextFileSync(parcelJson.toString()))
      const format = meta.format?.id
      const name = meta.name ?? '(unnamed)'
      if (format === FORMAT_NAME && meta.name) {
        log.info(name)
      }
    }
  })

  cli.command('sync')
    .description(`Sync gametest structures`)
    .action(async () => {
      const entries = FABRIC_GAMETEST_SAVED_PARCELS_DIR.listSync()
      let synced = 0
      let skipped = 0
      for (const entry of entries) {
        const metaFile = entry.join('parcel', 'parcel.json')
        if (!metaFile.existsSync()) {
          skipped++
          continue
        }
        const meta: ParcelMeta = JSON.parse(Deno.readTextFileSync(metaFile.toString()))
        if (meta.format?.id !== FORMAT_NAME) {
          skipped++
          continue
        }
        if (!meta.name || !isValidFilename(meta.name)) {
          skipped++
          continue
        }
        const nbtFile = entry.join('parcel', 'data', 'structure.nbt')
        if (!nbtFile.existsSync()) {
          log.warn(`Parcel ${meta.name}: structure.nbt not found, skipping`)
          skipped++
          continue
        }
        const targetFile = FABRIC_GAMETEST_STRUCTURE_DIR.join(`${meta.name}.nbt`)
        fs.copyFileSync(nbtFile, targetFile)
        log.info(`Synced ${meta.name}`)
        synced++
      }
      log.info(`Done: ${synced} synced, ${skipped} skipped`)
    })

  program.parse([Deno.execPath(), ...Deno.args])
}

if (import.meta.main) {
  await main()
}
