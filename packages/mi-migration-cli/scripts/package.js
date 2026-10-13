#!/usr/bin/env node
/**
 * Packages the CLI into a standalone binary using Node.js SEA
 * (Single Executable Applications, stable since Node.js 21).
 *
 * Steps:
 *   1. Bundle with ncc → dist/bundle/index.js
 *   2. Write sea-config.json
 *   3. node --experimental-sea-config → dist/sea-prep.blob
 *   4. Copy node binary → dist/mi-migrate[.exe]
 *   5. Remove codesig (macOS), inject blob with postject, re-sign
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const ROOT = path.resolve(__dirname, '..');
const BUNDLE = path.join(ROOT, 'dist', 'bundle', 'index.js');
const BLOB = path.join(ROOT, 'dist', 'sea-prep.blob');
const SEA_CONFIG = path.join(ROOT, 'sea-config.json');
const platform = os.platform();
const OUT_BIN = path.join(ROOT, 'dist', platform === 'win32' ? 'mi-migrate.exe' : 'mi-migrate');

function run(cmd, opts = {}) {
    console.log(`> ${cmd}`);
    execSync(cmd, { stdio: 'inherit', cwd: ROOT, ...opts });
}

// 1. Bundle
console.log('\n── Step 1: Bundle with ncc ──');
run('npx ncc build lib/index.js -o dist/bundle --minify');

// 2. SEA config
console.log('\n── Step 2: Write SEA config ──');
fs.writeFileSync(SEA_CONFIG, JSON.stringify({
    main: BUNDLE,
    output: BLOB,
    disableExperimentalSEAWarning: true,
}, null, 2));
console.log(`Written: ${SEA_CONFIG}`);

// 3. Generate blob
console.log('\n── Step 3: Generate SEA blob ──');
run(`node --experimental-sea-config ${SEA_CONFIG}`);

// 4. Copy node binary
console.log('\n── Step 4: Copy node binary ──');
const nodeBin = process.execPath;
fs.copyFileSync(nodeBin, OUT_BIN);
if (platform !== 'win32') fs.chmodSync(OUT_BIN, 0o755);
console.log(`Copied ${nodeBin} → ${OUT_BIN}`);

// 5. Inject blob
console.log('\n── Step 5: Inject blob with postject ──');
const FUSE = 'NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2';

if (platform === 'darwin') {
    run(`codesign --remove-signature "${OUT_BIN}"`);
    run(`npx postject "${OUT_BIN}" NODE_SEA_BLOB "${BLOB}" --sentinel-fuse ${FUSE} --macho-segment-name NODE_SEA`);
    run(`codesign --sign - "${OUT_BIN}"`);
} else if (platform === 'linux') {
    run(`npx postject "${OUT_BIN}" NODE_SEA_BLOB "${BLOB}" --sentinel-fuse ${FUSE}`);
} else {
    // Windows — postject works the same way, no signing step needed
    run(`npx postject "${OUT_BIN}" NODE_SEA_BLOB "${BLOB}" --sentinel-fuse ${FUSE}`);
}

// Cleanup temp files
fs.unlinkSync(BLOB);
fs.unlinkSync(SEA_CONFIG);

// Copy resources alongside the binary so dist/ is self-contained
console.log('\n── Step 6: Copy resources ──');
const resourcesSrc = path.join(ROOT, 'resources');
const resourcesDest = path.join(ROOT, 'dist', 'resources');
if (fs.existsSync(resourcesSrc)) {
    fs.cpSync(resourcesSrc, resourcesDest, { recursive: true, force: true });
    console.log(`Copied resources → ${resourcesDest}`);
} else {
    console.warn('WARNING: resources/ directory not found — run "npm run build" first');
}

console.log(`\n✓ Binary ready: ${OUT_BIN}`);
