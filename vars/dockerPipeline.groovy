//import groovy.transform.Field
//@Field

def call(Closure body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  echo "Pipeline configuration :\n${config}\n"

  def gitCommitId

  def mavenDockerImage = 'maven:3.5.4-jdk-8-alpine'
  def ansibleDockerImage = 'williamyeh/ansible:alpine3'

  pipeline {

    agent any

    environment {
      DOCKER_REGISTRY_USER = 'hvle'

      ARTIFACT_ID = readMavenPom().getArtifactId()
      ARTIFACT_VERSION = readMavenPom().getVersion()

      RUN_UNIT_TESTS = "${config.runUnitTests}"
      NEXUS_DEPLOY = "${config.nexusDeploy}"
      ANSIBLE_DEPLOY = "${config.ansibleDeploy}"
    }

    options {
      buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

      stage('Information') {
        steps {
          displayInformation("${ARTIFACT_ID}", "${ARTIFACT_VERSION}", "${gitCommitId}")
        }
      }

      stage('Docker image build and tag') {
        steps {
          sh "docker image build -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${gitCommitId} ."
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
        steps {
          script {
            withCredentials([usernamePassword(credentialsId: 'DOCKER_REGISTRY_CREDENTIAL',
                usernameVariable: 'DOCKER_REGISTRY_USER', passwordVariable: 'DOCKER_REGISTRY_PASSWORD')]) {
              sh 'docker login -u=${DOCKER_REGISTRY_USER} -p=${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY_SERVER}'
              sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION}'
              sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${gitCommitId}'
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
