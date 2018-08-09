def call() {
  withCredentials([usernamePassword(credentialsId: 'DOCKER_REGISTRY_CREDENTIAL',
      usernameVariable: 'DOCKER_REGISTRY_USER', passwordVariable: 'DOCKER_REGISTRY_PASSWORD')]) {
    sh 'docker login -u=${DOCKER_REGISTRY_USER} -p=${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY_SERVER}'
    sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION}'
    sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:' + gitCommitId()
    if ('master' == ${env.BRANCH_NAME}) {
      sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest'
    }
    sh 'docker logout'
  }
  //withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'DOCKER_REGISTRY_CREDENTIAL',
  //                  usernameVariable: 'DOCKER_REGISTRY_USER', passwordVariable: 'DOCKER_REGISTRY_PASSWORD']]) {
  //}
}
