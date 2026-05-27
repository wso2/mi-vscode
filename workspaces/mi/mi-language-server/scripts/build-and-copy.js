#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const projectRoot = path.resolve(__dirname, '..');
const extensionLsDir = path.resolve(projectRoot, '..', 'mi-extension', 'ls');

fs.rmSync(extensionLsDir, { recursive: true, force: true });
fs.mkdirSync(extensionLsDir, { recursive: true });

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
