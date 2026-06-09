const { existsSync } = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const repoRoot = path.resolve(__dirname, '..', '..');
const submodulePath = path.join(repoRoot, 'submodules', 'vscode-extensions');
const useRemote = process.argv.includes('--remote');

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
// Local development can opt into the latest submodule revision with --remote.
// CI intentionally omits that flag so builds use the gitlink pinned by the repo.
const submoduleUpdateArgs = ['submodule', 'update', '--init'];
if (useRemote) {
  submoduleUpdateArgs.push('--remote');
}
run('git', submoduleUpdateArgs);

if (existsSync(submodulePath)) {
  const sparsePaths = [
    '/workspaces/common-libs',
    '/workspaces/wso2-platform/wso2-platform-core',
    '/workspaces/rush-config.json'
  ];

  run('git', [
    '-C',
    submodulePath,
    'sparse-checkout',
    'set',
    '--no-cone',
    ...sparsePaths
  ]);
}
