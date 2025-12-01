import fs from 'fs';
import { ConventionalChangelog } from 'conventional-changelog';
import createPreset from 'conventional-changelog-conventionalcommits';
import { execSync } from 'node:child_process';

// Types
const types = [
    { type: 'feat', section: '⭐ Features:' },
    { type: 'fix', section: '🐛 Fixes:' },
    { type: 'perf', section: '⚡ Optimizations:' },
    { type: 'docs', section: '📖 Documentation:' },
    { type: 'style', section: '💅 Styling:' },
    { type: 'refactor', section: '♻️ Refactoring:' },
    { type: 'test', section: '🧪 Tests:' },
    { type: 'chore', section: '🧹 Chores:' }
];

// Get the release version tag
const releaseTag = process.argv[2];

// Create a preset
const preset = await createPreset({types});

// Configure the generator
const generator = new ConventionalChangelog()
    .readPackage()
    .config({
        ...preset,
        context: {
            version: releaseTag
        }
    });

// Produce the changelog
const stream = generator.writeStream();
stream.pipe(fs.createWriteStream('CHANGELOG.md'));
