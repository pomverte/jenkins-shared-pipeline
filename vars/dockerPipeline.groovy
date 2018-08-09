def call(Closure body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  echo "Pipeline configuration :\n${config}\n"

  pipeline {

    agent any

    environment {
      ARTIFACT_ID = readMavenPom().getArtifactId()
      ARTIFACT_VERSION = readMavenPom().getVersion()
    }

    options {
      buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

      stage('Information') {
        steps {
          displayInformation("${ARTIFACT_ID}", "${ARTIFACT_VERSION}")
        }
      }

      stage('Docker image build and tag') {
        steps {
          script {
            def sha1 = gitCommitId()
            sh "docker image build -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${sha1} ."
          }
        }
      }
      stage('Docker image tag latest') {
        when {
          branch 'master'
        }
        steps {
          sh 'docker image tag ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest'
        }
      }
      stage('Docker image push') {
        when {
          expression { return ${config.pushDockerImage} }
        }
        steps {
          script {
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
        }
      }

    }

    post {
      always {
        notifySlack "${currentBuild.currentResult}"
      }
    }

  }
}
