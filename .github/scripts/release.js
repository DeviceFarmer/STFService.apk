const assert = require('node:assert/strict')
const fs = require('node:fs')
const {spawnSync} = require('node:child_process')

const changelogPath = '.github/CHANGELOG.md'

function validateVersion(mode, version, manifest, gradle, ref, defaultBranch) {
  assert.ok(defaultBranch && ref === `refs/heads/${defaultBranch}`, `Run this on ${defaultBranch}`)
  assert.match(version, /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/, 'Use a version like 2.5.7, without a v prefix')
  const versionName = gradle.match(/versionName "([^"]+)"/)
  const versionCode = gradle.match(/versionCode (\d+)/)
  assert.ok(versionName && versionCode, 'Android versionName and versionCode must exist')
  assert.equal(versionName[1], manifest.version, 'Android and npm versions must match')
  if (mode === 'prepare') {
    const current = manifest.version.split('.').map(BigInt)
    const requested = version.split('.').map(BigInt)
    const changed = requested.findIndex((part, index) => part !== current[index])
    assert.ok(changed >= 0 && requested[changed] > current[changed], `${version} must be greater than ${manifest.version}`)
    assert.ok(Number(versionCode[1]) < 2100000000, 'Android versionCode cannot be incremented')
  }
  else {
    assert.equal(version, manifest.version, 'Merge the version bump before releasing')
  }
}

function prepareRelease(version, manifestText, gradle, changelog, notes) {
  assert.ok(notes.trim(), 'Generated release notes are empty')
  assert.ok(!changelog.includes(`\n## ${version}\n`), 'Changelog already contains this version')
  return {
    manifest: manifestText.replace(/"version":\s*"[^"]+"/, `"version": "${version}"`),
    gradle: gradle.replace(/versionName "[^"]+"/, `versionName "${version}"`)
      .replace(/versionCode (\d+)/, (_, code) => `versionCode ${Number(code) + 1}`),
    changelog: `# Changelog\n\n## ${version}\n\n${notes.replace(/\r\n/g, '\n').trim().replace(/^## /gm, '### ')}\n\n${changelog.replace(/^# Changelog\n*/, '')}`
  }
}

function releaseNotes(version, changelog) {
  const lines = changelog.split('\n')
  const start = lines.indexOf(`## ${version}`)
  assert.ok(start >= 0, `Changelog has no entry for ${version}`)
  const remaining = lines.slice(start + 1)
  const end = remaining.findIndex(line => line.startsWith('## '))
  const notes = remaining.slice(0, end < 0 ? remaining.length : end).join('\n').trim()
  assert.ok(notes, `Changelog entry for ${version} is empty`)
  return notes
}

async function checkUnpublished(version) {
  const tag = spawnSync('git', ['show-ref', '--verify', '--quiet', `refs/tags/v${version}`])
  assert.equal(tag.status, 1, `Tag v${version} already exists or tags could not be checked`)
  const response = await fetch(`https://registry.npmjs.org/@devicefarmer%2fstfservice-prebuilt/${version}`, {
    signal: AbortSignal.timeout(30000)
  })
  assert.equal(response.status, 404, `npm returned ${response.status}: this version is already published or the registry could not be checked`)
}

async function main() {
  const [command, notesPath] = process.argv.slice(2)
  const version = process.env.VERSION
  const changelog = fs.existsSync(changelogPath) ? fs.readFileSync(changelogPath, 'utf8') : '# Changelog\n'
  if (command === 'notes') {
    console.log(releaseNotes(version, changelog))
    return
  }
  assert.ok(['check-prepare', 'check-release', 'prepare'].includes(command), 'Unknown release command')
  const manifestText = fs.readFileSync('package.json', 'utf8')
  const gradle = fs.readFileSync('app/build.gradle', 'utf8')
  validateVersion(command === 'check-release' ? 'release' : 'prepare', version,
    JSON.parse(manifestText), gradle, process.env.GITHUB_REF, process.env.DEFAULT_BRANCH)
  if (command === 'prepare') {
    const next = prepareRelease(version, manifestText, gradle, changelog, fs.readFileSync(notesPath, 'utf8'))
    fs.writeFileSync('package.json', next.manifest)
    fs.writeFileSync('app/build.gradle', next.gradle)
    fs.writeFileSync(changelogPath, next.changelog)
    return
  }
  if (command === 'check-release') {
    releaseNotes(version, changelog)
  }
  await checkUnpublished(version)
}

module.exports = {validateVersion, prepareRelease, releaseNotes}

if (require.main === module) {
  main().catch(error => {
    console.error(error.message)
    process.exitCode = 1
  })
}
