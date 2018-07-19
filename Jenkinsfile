#!groovy

def dockerContainerRunMaven(mvnArgs){
  configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
    sh 'docker container run -t --rm --volume ${MAVEN_SETTINGS}:/tmp/settings.xml:ro --volume /maven/.m2:/root/.m2 --volume $PWD:/workspace --workdir /workspace maven:3.5.4-jdk-8-alpine mvn --batch-mode --settings /tmp/settings.xml ${mvnArgs}'
  }
}

def notifySlack(color, channel = '#jenkins-notifications') {
  slackSend channel: ${channel},
      color: ${color},
      message: "Build ${env.JOB_NAME} ${env.BUILD_NUMBER} status : ${currentBuild.currentResult}.\n${env.BUILD_URL}",
//    attachments: "",
      botUser: true
}

pipeline {

  agent any

  environment {
    RUN_UNIT_TESTS = 'true'
    ARTIFACT_ID = readMavenPom().getArtifactId()
    ARTIFACT_VERSION = readMavenPom().getVersion()
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
  }

  stages {

    stage('Information') {
      steps {
        script {
          def commit = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%h\'  origin/' + env.BRANCH_NAME).trim()
          def author = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%an\' origin/' + env.BRANCH_NAME).trim()
          def authorEmail = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%ae\' origin/' + env.BRANCH_NAME).trim()
          def comment = sh(returnStdout: true, script: 'git --no-pager show -s --format=\'%B\' origin/' + env.BRANCH_NAME).trim()
          echo """
            Branch : ${env.BRANCH_NAME}
            Author : ${author}
            Email : ${authorEmail}
            Commit : ${commit}
            Comment : ${comment}
            ArtifactId : ${ARTIFACT_ID}
            Version : ${ARTIFACT_VERSION}
          """
        }
      }
    }

    stage('Build Package') {
      when {
        environment name: 'RUN_UNIT_TESTS', value: 'true'
      }
      steps {
        dockerContainerRunMaven 'clean package'
      }
      when {
        not { environment name: 'RUN_UNIT_TESTS', value: 'true' }
      }
      steps {
        dockerContainerRunMaven 'clean package -DskipTests'
      }
    }

    stage('Parallel Stages') {
      parallel {
        stage('Deploy Artifact') {
          steps {
            dockerContainerRunMaven 'deploy -DskipTests'
          }
        }
        stage('SonarQube Analyze') {
          steps {
            echo 'TODO run SonarQube'
            //dockerContainerRunMaven 'sonar:sonar -Dsonar.branch=${env.BRANCH_NAME}'
          }
        }
      }
    }

    stage('Deploy to environment') {
      when {
        expression {
          echo 'Should I deploy ?'
          return false
        }
        // deploy to env
        //branch 'develop'
      }
      steps {
        echo 'TODO'
      }
    }

    post {
      success {
        notifySlack 'good'
      }
      failure {
        // TODO send mail
        notifySlack 'danger'
      }
    }

  }
}
