import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDirectory = dirname(fileURLToPath(import.meta.url))
const outputPath = resolve(scriptDirectory, '../openapi/incident-hub.json')
const apiDocsUrl =
  process.env.OPENAPI_URL ?? 'http://localhost:8080/v3/api-docs'

const response = await fetch(apiDocsUrl)

if (!response.ok) {
  throw new Error(`OpenAPI request failed with status ${response.status}`)
}

const document = await response.json()

await mkdir(dirname(outputPath), { recursive: true })
await writeFile(outputPath, `${JSON.stringify(document, null, 2)}\n`, 'utf8')

console.log(`OpenAPI snapshot updated from ${apiDocsUrl}`)
