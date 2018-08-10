String call() {
  sh(returnStdout: true, script: 'git describe --tags --abbrev=0').trim()
}
