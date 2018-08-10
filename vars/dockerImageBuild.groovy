def call() {
  def sha1 = gitCommitId()
  sh "docker image build --label 'git.commit.id=${sha1}' -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} ."
  if ('master' == "${env.BRANCH_NAME}") {
    sh "docker image tag ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest"
  }
}
