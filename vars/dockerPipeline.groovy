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
      DOCKER_REGISTRY_SERVER = ''
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
          dockerImageBuild()
        }
      }
      stage('Docker image push') {
        when {
          expression { return ${config.pushDockerImage} }
        }
        steps {
          dockerImagePush()
        }
      }

    }

    post {
      always {
        notifySlack "${currentBuild.currentResult}", "#graylog-notifications"
      }
    }

  }
}
