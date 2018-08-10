String call(String sha = 'HEAD') {
  sh(returnStdout: true, script: 'git --no-pager show -s --format=%an ' + sha).trim()
}
