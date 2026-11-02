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
const MANIFEST_TMP = path.join(ROOT, 'dist', '__asset_manifest__.json');
const platform = os.platform();
const OUT_BIN = path.join(ROOT, 'dist', platform === 'win32' ? 'mi-migrate.exe' : 'mi-migrate');

// Collect all files under resources/ as SEA assets.
// Also writes a __asset_manifest__ asset so the runtime knows what to extract.
function collectAssets(dir, prefix, assets, keys) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const fullPath = path.join(dir, entry.name);
        const key = prefix ? `${prefix}/${entry.name}` : entry.name;
        if (entry.isDirectory()) {
            collectAssets(fullPath, key, assets, keys);
        } else {
            assets[key] = fullPath;
            keys.push(key);
        }
    }
}

function buildSeaAssets() {
    const assets = {};
    const keys = [];
    const resourcesSrc = path.join(ROOT, 'resources');
    if (fs.existsSync(resourcesSrc)) {
        collectAssets(resourcesSrc, '', assets, keys);
    } else {
        console.error('ERROR: resources/ not found — run "npm run build" first');
        process.exit(1);
    }
    fs.writeFileSync(MANIFEST_TMP, JSON.stringify(keys));
    assets['__asset_manifest__'] = MANIFEST_TMP;
    return assets;
}

function run(cmd, opts = {}) {
    console.log(`> ${cmd}`);
    execSync(cmd, { stdio: 'inherit', cwd: ROOT, ...opts });
}

// 1. Bundle
console.log('\n── Step 1: Bundle with ncc ──');
run('npx ncc build lib/index.js -o dist/bundle --minify');

// 2. SEA config (with embedded resource assets)
console.log('\n── Step 2: Write SEA config ──');
const assets = buildSeaAssets();
fs.writeFileSync(SEA_CONFIG, JSON.stringify({
    main: BUNDLE,
    output: BLOB,
    disableExperimentalSEAWarning: true,
    assets,
}, null, 2));
console.log(`Written: ${SEA_CONFIG} (${Object.keys(assets).length - 1} resource asset(s) + manifest)`);

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
fs.unlinkSync(MANIFEST_TMP);

console.log(`\n✓ Binary ready (resources embedded): ${OUT_BIN}`);
