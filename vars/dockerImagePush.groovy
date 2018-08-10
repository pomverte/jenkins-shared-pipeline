def call() {
  withCredentials([string(credentialsId: 'DOCKER_REGISTRY_PASSWORD', variable: 'DOCKER_REGISTRY_PASSWORD')]) {
//    sh "docker login -u=${DOCKER_REGISTRY_USER} -p=${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY_SERVER}"
    sh "docker login -u=${DOCKER_REGISTRY_USER} -p=${DOCKER_REGISTRY_PASSWORD}"
    sh "docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION}"
    if ('master' == "${env.BRANCH_NAME}") {
      sh "docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest"
    }
    sh 'docker logout'
  }
}
