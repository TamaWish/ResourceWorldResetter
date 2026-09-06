import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const projectRoot = resolve(import.meta.dirname, '..', '..');
const sourceDirectory = resolve(projectRoot, 'docs', 'public');
const targetDirectory = resolve(import.meta.dirname, '..', 'src', 'content', 'docs', 'reference');
const guides = [
  ['OPERATIONS_AND_MIGRATION.md', 'operations-and-migration.md', 'Operations & migration', 'The public runbook for installing, operating, recovering, and migrating RWR 5.'],
  ['RELEASE_NOTES.md', 'release-notes.md', 'Release notes', 'New features, fixes, upgrade notes, and compatibility details for ResourceWorldResetter.'],
];

await rm(resolve(projectRoot, 'website', '.astro'), { recursive: true, force: true });
await mkdir(targetDirectory, { recursive: true });

for (const [sourceName, targetName, title, description] of guides) {
  let body = await readFile(resolve(sourceDirectory, sourceName), 'utf8');
  body = body.replace(/\[CHANGELOG_v4\.md\]\(\.\.\/\.\.\/CHANGELOG_v4\.md\)/g, '[legacy v4 changelog](https://github.com/TamaWish/ResourceWorldResetter/blob/main/CHANGELOG_v4.md)');
  body = body.replace(/\[Contributing\]\(\.\.\/\.\.\/CONTRIBUTING\.md\)/g, '[Contributing](https://github.com/TamaWish/ResourceWorldResetter/blob/main/CONTRIBUTING.md)');
  body = body.replace(/^# .+\r?\n(?:\r?\n)*/, '');
  body = body.replace(/^Operator-facing notes for the \*\*v5\*\* line\. Every \*\*5\.x\.x\*\* release is recorded here, newest first\. When v6 begins, start `RELEASE_NOTES_v6\.md`\.\r?\n+/,
    'See what changed in each ResourceWorldResetter release, including upgrade notes and compatibility requirements.\n\n');
  await writeFile(
    resolve(targetDirectory, targetName),
    `---\ntitle: ${title}\ndescription: ${description}\n---\n\n${body}`,
  );
}
