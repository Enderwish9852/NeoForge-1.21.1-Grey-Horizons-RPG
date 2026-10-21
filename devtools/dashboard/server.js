'use strict';
/*
 * Grey Horizons RPG - dev dashboard
 * Serves a live side task bar: current tasks + files edited in the working tree.
 *   http://localhost:7645            -> dashboard
 *   http://localhost:7645/api/state  -> JSON (tasks + git working tree)
 *
 * Run:  node devtools/dashboard/server.js
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const DASH_DIR = __dirname;
const ROOT = path.resolve(DASH_DIR, '..', '..');
const PORT = Number(process.env.PORT || 7645);
const STATE_FILE = path.join(DASH_DIR, 'state.json');
const HTML_FILE = path.join(DASH_DIR, 'dashboard.html');

const DEFAULT_STATE = {
  project: 'Grey Horizons RPG',
  subtitle: 'NeoForge 1.21.1',
  currentTask: '',
  tasks: [],
  files: [],
  notes: []
};

function readState() {
  try {
    const raw = fs.readFileSync(STATE_FILE, 'utf8');
    const parsed = JSON.parse(raw);
    return Object.assign({}, DEFAULT_STATE, parsed);
  } catch (e) {
    return Object.assign({}, DEFAULT_STATE);
  }
}

function git(args) {
  try {
    // Only strip trailing whitespace: the leading status column of the first
    // porcelain line is meaningful (" M path").
    return execFileSync('git', ['-C', ROOT].concat(args), {
      encoding: 'utf8',
      timeout: 10000,
      stdio: ['ignore', 'pipe', 'ignore']
    }).replace(/[\r\n]+$/, '');
  } catch (e) {
    return '';
  }
}

function classify(code) {
  if (code === '??') return 'new';
  if (code.indexOf('D') >= 0) return 'deleted';
  if (code.indexOf('R') >= 0) return 'renamed';
  if (code.indexOf('A') >= 0) return 'added';
  return 'modified';
}

function gitState() {
  const status = git(['status', '--porcelain']);
  const workingFiles = status
    .split('\n')
    .filter(Boolean)
    .map((line) => {
      const code = line.slice(0, 2);
      let file = line.slice(3).trim();
      if (file.indexOf(' -> ') >= 0) file = file.split(' -> ')[1];
      return {
        path: file,
        action: classify(code),
        module: file.split('/')[0],
        size: fileSize(file)
      };
    });

  let diffStat = [];
  const stat = git(['diff', '--numstat']);
  if (stat) {
    diffStat = stat
      .split('\n')
      .filter(Boolean)
      .map((line) => {
        const parts = line.split('\t');
        return { path: parts[2], added: Number(parts[0]) || 0, removed: Number(parts[1]) || 0 };
      });
  }

  return {
    branch: git(['rev-parse', '--abbrev-ref', 'HEAD']),
    lastCommit: git(['log', '-1', '--pretty=%h %s']),
    commits: git(['log', '-5', '--pretty=%h %s']),
    workingFiles: workingFiles,
    diffStat: diffStat
  };
}

function fileSize(rel) {
  try {
    return fs.statSync(path.join(ROOT, rel)).size;
  } catch (e) {
    return null;
  }
}

function send(res, code, type, body) {
  res.writeHead(code, {
    'Content-Type': type,
    'Cache-Control': 'no-store',
    'Access-Control-Allow-Origin': '*'
  });
  res.end(body);
}

const server = http.createServer((req, res) => {
  const url = (req.url || '/').split('?')[0];

  if (url === '/' || url === '/index.html') {
    try {
      send(res, 200, 'text/html; charset=utf-8', fs.readFileSync(HTML_FILE));
    } catch (e) {
      send(res, 500, 'text/plain', 'dashboard.html missing: ' + e.message);
    }
    return;
  }

  if (url === '/api/state') {
    const state = readState();
    const payload = Object.assign({}, state, {
      git: gitState(),
      now: new Date().toISOString(),
      root: ROOT
    });
    send(res, 200, 'application/json; charset=utf-8', JSON.stringify(payload));
    return;
  }

  if (url === '/api/reload') {
    send(res, 200, 'application/json', JSON.stringify({ ok: true }));
    return;
  }

  send(res, 404, 'text/plain', 'not found');
});

server.listen(PORT, '127.0.0.1', () => {
  console.log('Grey Horizons dev dashboard on http://localhost:' + PORT);
  console.log('Project root: ' + ROOT);
});
