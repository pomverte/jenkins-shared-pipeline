/**
 * Build a docker image in working directory.
 * Tag it with a maven version, labels are added with author and git commit id.
 * In case of master branch, tag latest is also added.
 */
def build() {
  def author = gitme.commitAuthor()
  def sha1 = gitme.commitId()
  configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
    sh "cp ${MAVEN_SETTINGS} settings.xml"
    sh "docker image build --label 'maintainer=${author}' --label 'git.commit.id=${sha1}' -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} ."
  }
  if ('master' == "${env.BRANCH_NAME}") {
    sh "docker image tag ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest"
  }
}

/**
 * Log into docker registry and push tags (lastest tag is pushed in case of master branch).
 * Logout after operation.
 */
def push() {
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
