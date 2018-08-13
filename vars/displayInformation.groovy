def call(String artifactId, String artifactVersion) {

  def gitCommitId = gitme.commitId()
  def commitAuthor = gitme.commitAuthor()
  def commitMessage = gitme.commitMessage()
  echo """

Branch : ${env.BRANCH_NAME}
Author : ${commitAuthor}
Commit : ${gitCommitId}
Comment : ${commitMessage}
ArtifactId : ${artifactId}
Version : ${artifactVersion}
"""
}
