def call(String artifactId, String artifactVersion, String gitCommitId) {

  gitCommitId = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
  def author = sh(returnStdout: true, script: 'git --no-pager show -s --format=%an HEAD').trim()
  def authorEmail = sh(returnStdout: true, script: 'git --no-pager show -s --format=%ae HEAD').trim()
  def commitMessage = sh(returnStdout: true, script: 'git --no-pager show -s --format=%B HEAD').trim()

  echo """

Branch : ${env.BRANCH_NAME}
Author : ${author}
Email : ${authorEmail}
Commit : ${gitCommitId}
Comment : ${commitMessage}
ArtifactId : ${artifactId}
Version : ${artifactVersion}
"""
}