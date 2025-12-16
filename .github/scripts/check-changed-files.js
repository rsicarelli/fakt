/**
 * Check if PR contains only documentation changes
 * @param {Object} github - GitHub API client
 * @param {Object} context - GitHub Actions context
 * @param {Object} core - GitHub Actions core
 * @param {Array<string>} skipExtensions - File extensions that should skip CI
 * @returns {Promise<boolean>} - true if CI should run, false if should skip
 */
module.exports = async ({ github, context, core, skipExtensions = ['.md', '.txt'] }) => {
  const { data: files } = await github.rest.pulls.listFiles({
    owner: context.repo.owner,
    repo: context.repo.repo,
    pull_number: context.issue.number,
  });

  console.log(`📊 Total files changed in PR: ${files.length}`);
  console.log('📁 All changed files:');
  files.forEach(f => console.log(`  - ${f.filename} (${f.status})`));

  // Check if ALL files match skip extensions
  const allFilesAreSkippable = files.every(f =>
    skipExtensions.some(ext => f.filename.endsWith(ext))
  );

  const hasChanges = !allFilesAreSkippable;
  core.setOutput('has-changes', hasChanges ? 'true' : 'false');

  if (hasChanges) {
    console.log('\n✅ Code changes detected - CI will run');
    const codeFiles = files.filter(f =>
      !skipExtensions.some(ext => f.filename.endsWith(ext))
    );
    console.log(`🔧 Files requiring CI validation (${codeFiles.length}):`);
    codeFiles.forEach(f => console.log(`  - ${f.filename}`));
  } else {
    console.log('\n⏭️  Only skippable files changed - CI will be skipped');
    console.log(`📝 All files match skip extensions: ${skipExtensions.join(', ')}`);
    console.log('💡 All validation jobs will be skipped');
  }

  return hasChanges;
};
