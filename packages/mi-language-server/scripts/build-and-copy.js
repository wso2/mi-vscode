#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const projectRoot = path.resolve(__dirname, '..');
const extensionRoot = path.resolve(projectRoot, '..', 'mi-extension');
const extensionLsDir = path.join(extensionRoot, 'ls');
const downloadScript = path.join(extensionRoot, 'scripts', 'download-ls.js');
const shouldDownloadLs = ['1', 'true', 'yes'].includes(
  String(process.env.MI_DOWNLOAD_LS || '').toLowerCase()
);

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    stdio: 'inherit',
    ...options
  });

  if (result.error) {
    console.error(`Failed to start ${command}: ${result.error.message}`);
    process.exit(1);
  }

  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}

fs.rmSync(extensionLsDir, { recursive: true, force: true });
fs.mkdirSync(extensionLsDir, { recursive: true });

if (shouldDownloadLs) {
  console.log(`MI_DOWNLOAD_LS is enabled. Downloading language server into ${extensionLsDir}`);
  run(process.execPath, [downloadScript], { cwd: extensionRoot, env: process.env });
  process.exit(0);
}

const command = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';
const result = spawnSync(command, ['clean', 'package', '-DskipTests'], {
  cwd: projectRoot,
  stdio: 'inherit',
  shell: process.platform === 'win32'
});

if (result.error) {
  console.error(`Failed to start Maven wrapper: ${result.error.message}`);
  process.exit(1);
}

if (result.status !== 0) {
  process.exit(result.status || 1);
}

const targetDir = path.join(projectRoot, 'org.eclipse.lemminx', 'target');
const jarFiles = fs.readdirSync(targetDir).filter(file => file.endsWith('.jar'));

if (!jarFiles.length) {
  console.error(`No language server jars found in ${targetDir}`);
  process.exit(1);
}

for (const jarFile of jarFiles) {
  fs.copyFileSync(path.join(targetDir, jarFile), path.join(extensionLsDir, jarFile));
}

console.log(`Copied ${jarFiles.length} language server jar(s) to ${extensionLsDir}`);
