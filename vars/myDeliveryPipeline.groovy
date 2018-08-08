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
          displayInformation
        }
      }

      stage('Build Package with Tests') {
        when {
          environment name: 'RUN_UNIT_TESTS', value: 'true'
        }
        agent {
          docker {
            image "${mavenDockerImage}"
            args '-v $HOME/.m2:/root/.m2'
          }
        }
        steps {
          runMavenGoal 'clean package'
        }
      }
      stage('Build Package Skip Tests') {
        when {
          not { environment name: 'RUN_UNIT_TESTS', value: 'true' }
        }
        agent {
          docker {
            image "${mavenDockerImage}"
            args '-v $HOME/.m2:/root/.m2'
          }
        }
        steps {
          runMavenGoal 'clean package -DskipTests'
        }
      }

      stage('Deploy Artifact') {
        when {
          environment name: 'NEXUS_DEPLOY', value: 'true'
        }
        agent {
          docker {
            image 'maven:3.5.4-jdk-8-alpine'
            args '-v $HOME/.m2:/root/.m2'
            reuseNode true
          }
        }
        steps {
          configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh 'mvn -B -s ${MAVEN_SETTINGS} deploy -DskipTests'
          }
        }
      }

      stage('SonarQube') {
        steps {
          echo "TODO Static Analysis ..."
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
          echo 'TODO docker image push ...'
          //sh 'docker login -u=${DOCKER_USER} -p=${DOCKER_PASSWORD} ${DOCKER_REGISTRY}'
          //sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION}'
          //sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${gitCommitId}'
          //sh 'docker image push ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:latest'
          //sh 'docker logout'
        }
      }

      stage('Ansible deploy') {
        when {
          environment name: 'ANSIBLE_DEPLOY', value: 'true'
        }
        agent {
          docker {
            image "${ansibleDockerImage}"
          }
        }
        steps {
          sh 'ansible-playbook --version'
        }
      }
    }

    post {
      always {
        notifySlack ${currentBuild.currentResult}
      }
    }

  }
}
