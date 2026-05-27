const { existsSync } = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const repoRoot = path.resolve(__dirname, '..', '..');
const submodulePath = path.join(repoRoot, 'submodules', 'vscode-extensions');

function run(command, args, cwd = repoRoot) {
  const result = spawnSync(command, args, { cwd, stdio: 'inherit' });

  if (result.error) {
    console.error(result.error.message);
    process.exit(1);
  }

  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}

run('git', ['submodule', 'sync']);
run('git', ['submodule', 'update', '--init', '--remote']);

if (existsSync(submodulePath)) {
  run('git', [
    '-C',
    submodulePath,
    'sparse-checkout',
    'set',
    '--no-cone',
    '/workspaces/common-libs',
    '/workspaces/rush-config.json'
  ]);
}
