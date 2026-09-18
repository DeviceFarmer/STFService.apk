const assert = require('node:assert/strict')
const childProcess = require('node:child_process')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const {test} = require('node:test')
const {validateVersion, prepareRelease, releaseNotes} = require('./release')

const manifest = {version: '2.5.6'}
const gradle = 'versionCode 15\nversionName "2.5.6"\n'

test('the release workflow publishes a local archive', (t) => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'stfservice-publish-'))
  t.after(() => fs.rmSync(directory, {recursive: true, force: true}))
  fs.mkdirSync(path.join(directory, 'dist'))
  fs.writeFileSync(path.join(directory, 'package.json'), JSON.stringify({name: 'stfservice-release-test', version: '0.0.0'}))
  const packed = childProcess.spawnSync('npm', ['pack', '--ignore-scripts', '--offline', '--pack-destination', 'dist'], {
    cwd: directory,
    encoding: 'utf8'
  })
  assert.equal(packed.status, 0, packed.stderr)
  const workflow = fs.readFileSync(path.join(__dirname, '../workflows/release.yml'), 'utf8')
  const publish = workflow.match(/^        run: (npm publish .+)$/m)
  assert.ok(publish, 'The release workflow must have an npm publish command')
  const result = childProcess.spawnSync('bash', ['-c', `${publish[1]} --dry-run --offline --ignore-scripts --provenance=false`], {
    cwd: directory,
    encoding: 'utf8',
    env: {...process.env, GIT_SSH_COMMAND: 'false', GIT_TERMINAL_PROMPT: '0'}
  })
  assert.equal(result.status, 0, result.stderr)
  assert.match(result.stdout, /\+ stfservice-release-test@0\.0\.0/)
})

test('the manifest repository matches the GitHub repository casing', () => {
  const published = JSON.parse(fs.readFileSync(path.join(__dirname, '../../package.json'), 'utf8'))
  assert.equal(published.repository.url, 'git+https://github.com/DeviceFarmer/STFService.apk.git',
    'npm rejects the provenance bundle unless repository.url matches the GitHub repository, including its casing')
})

test('prepare requires an increasing stable version and the default branch', () => {
  for (const version of ['2.5.7', '2.10.0', '3.0.0']) {
    validateVersion('prepare', version, manifest, gradle, 'refs/heads/master', 'master')
  }
  for (const version of ['2.5.6', '2.5.5', '1.10.0', 'v2.5.7', '2.5.7-beta.1', '02.5.7', '2.5', '2.5.7\n']) {
    assert.throws(() => validateVersion('prepare', version, manifest, gradle, 'refs/heads/master', 'master'))
  }
  for (const ref of ['refs/heads/release/v2.5.7', 'refs/tags/master']) {
    assert.throws(() => validateVersion('prepare', '2.5.7', manifest, gradle, ref, 'master'))
  }
})

test('release rejects an unmerged bump, another branch and Android version drift', () => {
  validateVersion('release', '2.5.6', manifest, gradle, 'refs/heads/master', 'master')
  assert.throws(() => validateVersion('release', '2.5.7', manifest, gradle, 'refs/heads/master', 'master'))
  assert.throws(() => validateVersion('release', '2.5.6', manifest, gradle, 'refs/heads/release/v2.5.6', 'master'))
  assert.throws(() => validateVersion('release', '2.5.6', manifest, gradle.replace('2.5.6', '2.5.5'), 'refs/heads/master', 'master'))
  assert.throws(() => validateVersion('prepare', '2.5.7', manifest, gradle.replace('15', '2100000000'), 'refs/heads/master', 'master'))
})

test('prepare updates both versions, increments versionCode and preserves earlier notes', () => {
  const notes = "## What's Changed\r\n* Fix rotation\r\n\r\n## New Contributors\r\n* @example\r\n"
  const previous = '# Changelog\n\n## 2.5.6\n\nEarlier notes\n'
  const next = prepareRelease('2.5.7', JSON.stringify(manifest, null, 2), gradle, previous, notes)
  assert.equal(JSON.parse(next.manifest).version, '2.5.7')
  assert.equal(next.gradle, 'versionCode 16\nversionName "2.5.7"\n')
  assert.equal(releaseNotes('2.5.7', next.changelog), "### What's Changed\n* Fix rotation\n\n### New Contributors\n* @example")
  assert.equal(releaseNotes('2.5.6', next.changelog), 'Earlier notes')
  validateVersion('release', '2.5.7', JSON.parse(next.manifest), next.gradle, 'refs/heads/master', 'master')
})

test('release requires nonempty reviewed notes and prepare cannot duplicate an entry', () => {
  assert.throws(() => releaseNotes('2.5.7', '# Changelog\n'))
  assert.throws(() => releaseNotes('2.5.7', '# Changelog\n\n## 2.5.7\n\n## 2.5.6\nOlder\n'))
  assert.throws(() => prepareRelease('2.5.7', '{}', gradle, '# Changelog\n', ' '))
  assert.throws(() => prepareRelease('2.5.7', '{}', gradle, '# Changelog\n\n## 2.5.7\nExisting\n', 'New'))
})
