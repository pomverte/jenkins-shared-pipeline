String commitId(String sha = 'HEAD') {
  sh(returnStdout: true, script: 'git rev-parse --short ' + sha).trim()
}

String show(String format, String sha = 'HEAD') {
  sh(returnStdout: true, script: "git --no-pager show -s --format=\'${format}\' " + sha).trim()
}

String commitMessage(String sha = 'HEAD') {
  show('%B', sha)
}

String commitAuthor(String sha = 'HEAD') {
  show('%an <%ae>', sha)
}

String commitAuthorName(String sha = 'HEAD') {
  show('%an', sha)
}

String latestTag() {
  sh(returnStdout: true, script: 'git describe --tags --abbrev=0').trim()
}
