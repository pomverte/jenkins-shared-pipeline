def call(body) {
  echo "${body}"
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  echo "${config}"
  body()

  pipeline {

    agent any

    environment {
      RUN_UNIT_TESTS = "${config.runUnitTests}"
      DEPLOY_ARTIFACT = "${config.deployArtifact}"
      ARTIFACT_ID = readMavenPom().getArtifactId()
      ARTIFACT_VERSION = readMavenPom().getVersion()
      DOCKER_REGISTRY_USER = 'sirh'
    }

    options {
      buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

      stage('Information') {
        steps {
          script {
            def gitCommitId = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
            def author = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%an\' origin/' + env.BRANCH_NAME).trim()
            def authorEmail = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%ae\' origin/' + env.BRANCH_NAME).trim()
            def comment = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%B\' origin/' + env.BRANCH_NAME).trim()
            echo """
    Branch : ${env.BRANCH_NAME}
    Author : ${author}
    Email : ${authorEmail}
    Commit : ${gitCommitId}
    Comment : ${comment}
    ArtifactId : ${ARTIFACT_ID}
    Version : ${ARTIFACT_VERSION}
          """
          }
        }
      }

      stage('Build Package with Tests') {
        when {
          environment name: 'RUN_UNIT_TESTS', value: 'true'
        }
        agent {
          docker {
            image 'maven:3.5.4-jdk-8-alpine'
            args '-v $HOME/.m2:/root/.m2'
          }
        }
        steps {
          configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh 'mvn -B -s ${MAVEN_SETTINGS} clean package'
          }
        }
      }
      stage('Build Package Skip Tests') {
        when {
          not { environment name: 'RUN_UNIT_TESTS', value: 'true' }
        }
        agent {
          docker {
            image 'maven:3.5.4-jdk-8-alpine'
            args '-v $HOME/.m2:/root/.m2'
          }
        }
        steps {
          configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
            sh 'mvn -B -s ${MAVEN_SETTINGS} clean package -DskipTests'
          }
        }
      }

      stage('Deploy Artifact') {
        when {
          environment name: 'DEPLOY_ARTIFACT', value: 'true'
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
          script {
            def gitCommitId = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
            sh "docker image build -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION} -t ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${gitCommitId} ."
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
      // TODO docker login push

    }

    post {
      success {
        echo "Build ${env.JOB_NAME} #${env.BUILD_NUMBER} status : ${currentBuild.currentResult}.\n${env.BUILD_URL}"
        // TODO send slack
//        slackSend channel: '#jenkins',
//            color: 'good',
//            message: "Build ${env.JOB_NAME} ${env.BUILD_NUMBER} status : ${currentBuild.currentResult}.\n${env.BUILD_URL}",
//            attachments: "",
//            botUser: true
      }
      failure {
        // TODO send mail / slack
        echo "I have not failed. I've just found 10 000 ways that won't work. -Thomas Edison"
        echo "Failure is unimportant. It takes courage to make a fool of yourself. -Charlie Chaplin"
      }
    }

  }
}