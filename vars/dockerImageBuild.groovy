def call() {
  withCredentials([usernamePassword(credentialsId: 'DOCKER_REGISTRY_CREDENTIAL',
      usernameVariable: 'DOCKER_REGISTRY_USER', passwordVariable: 'DOCKER_REGISTRY_PASSWORD')]) {
    def sha1 = gitCommitId()
    sh "docker image build -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${sha1} ."
    if ('master' == ${env.BRANCH_NAME}) {
      sh 'docker image tag ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest'
    }
  }
}
