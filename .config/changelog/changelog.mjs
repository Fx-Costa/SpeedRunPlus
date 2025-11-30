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

// Clean the commit message from titles, SNAPSHOT-tags, etc. these are not included in
// the conventional commit specification - but utilities in this project
const commitTransformer = (commit) => {
    let newCommit = { ...commit };

    // Decide whether the commit message is titled or marked as a snapshot
    const isConventional = commit.type !== null && types.some(t => commit.header.startsWith(`${t.type}:`));
    const isSnapshot = commit.footer && commit.footer.endsWith("[SNAPSHOT]");

    if (isSnapshot) {
        // Remove "[SNAPSHOT]" from footer and notes
        newCommit.footer = newCommit.footer.replace(/\[SNAPSHOT\]/g, "").trim();
        newCommit.notes = newCommit.notes.map(note => ({
          ...note,
          text: note.text.replace(/\[SNAPSHOT\]/g, "").trim()
        }));
    }

    if (!isConventional){
        // Build using the first entry in the body
        newCommit.header = newCommit.body.split('\n')[0].trim();
        newCommit.body = "";

        // Parse the header
        const match = newCommit.header.match(/^([a-z]+)(\(([^)]+)\))?:\s*(.+)$/);
        if (!match) return newCommit;
        const [, type, , scope, subject] = match;
        newCommit.type = type;
        newCommit.scope = scope;
        newCommit.subject = subject;
    }

    return newCommit;
}


const preset = await createPreset({types});
const originalTransform = preset.writer.transform;
preset.writer.transform = (commit, context) => {
    // Skip certain commits
    if (commit.message && commit.message.includes("[skip ci]")) {
        return undefined;
    }

    const cleaned = commitTransformer(commit);
    return originalTransform
        ? originalTransform(cleaned, context)
        : cleaned;
};

// Configure generator
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
