String call(String sha = 'HEAD') {
  sh(returnStdout: true, script: 'git rev-parse --short ' + sha).trim()
}
