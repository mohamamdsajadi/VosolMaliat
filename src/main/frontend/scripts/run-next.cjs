const { existsSync } = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const frontendRoot = path.resolve(__dirname, '..');
const localNode = path.join(frontendRoot, 'node',
  process.platform === 'win32' ? 'node.exe' : 'bin/node');
const runtime = existsSync(localNode) ? localNode : process.execPath;
const version = spawnSync(runtime, ['--version'], { encoding: 'utf8' });
if (version.error || version.status !== 0) {
  console.error(`Cannot run Node.js at ${runtime}: ${version.error?.message || version.stderr}`);
  process.exit(1);
}
const [major, minor] = version.stdout.trim().replace(/^v/, '').split('.').map(Number);
if (!(major > 20 || (major === 20 && minor >= 9))) {
  console.error('Next.js requires Node.js 20.9 or newer. Install Node.js 22, or run the Maven frontend install-node-and-npm step.');
  process.exit(1);
}

// Child processes must use the same runtime, even when npm was launched by an older Node.
const env = { ...process.env };
const pathKey = Object.keys(env).find(key => key.toLowerCase() === 'path') || 'PATH';
env[pathKey] = path.dirname(runtime) + path.delimiter + (env[pathKey] || '');
const result = spawnSync(runtime, [
  require.resolve('next/dist/bin/next'), ...process.argv.slice(2)
], { cwd: frontendRoot, env, stdio: 'inherit' });
if (result.error) console.error(result.error.message);
process.exit(result.status ?? 1);
