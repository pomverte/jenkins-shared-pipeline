// maybe we could use :
// - https://qa.nuxeo.org/jenkins/pipeline-syntax/globals#docker
// - https://jenkins.io/doc/book/pipeline/docker/

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
      //DOCKER_REGISTRY_SERVER = ''
      DOCKER_REGISTRY_USER = "vietnem"
      PUSH_DOCKER_IMAGE = "${config.pushDockerImage}"
      RUN_DOCKER_CONTAINER = "${config.runDockerContainer}"
      ARTIFACT_ID = readMavenPom().getArtifactId()
      ARTIFACT_VERSION = readMavenPom().getVersion()
    }

    options {
      disableConcurrentBuilds()
      buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

      stage('Information') {
        steps {
          displayInformation("${ARTIFACT_ID}", "${ARTIFACT_VERSION}")
        }
      }

      stage('Linting') {
        environment {
          HADOLINT_DOCKER_IMAGE = 'hadolint/hadolint:v1.10.4'
          HADOLINT_DOCKER_CMD = 'hadolint --ignore DL3018 --ignore DL3013 /app/Dockerfile'
          JENKINS_VOLUME = 'gitea_jenkins_jenkins_home' // FIXME hard coded docker volume name here ...
        }
        steps {
          script {
            def path = pwd().split('/')
            def workspace = "/var/lib/docker/volumes/${JENKINS_VOLUME}/_data/workspace/" + path[path.size() - 1]
            sh "docker container run --rm -i -v ${workspace}/Dockerfile:/app/Dockerfile ${HADOLINT_DOCKER_IMAGE} ${HADOLINT_DOCKER_CMD}"
          }
        }
      }

      stage('Image build and tag') {
        steps {
          script {
            dockerImage.build()
          }
        }
      }

      stage('Image push') {
        when {
          environment name: 'PUSH_DOCKER_IMAGE', value: 'true'
        }
        steps {
          script {
            dockerImage.push()
          }
        }
      }

      // FIXME THIS STAGE IS TEMPORARY
      // TODO call WORKSHOP
      stage('Container run') {
        when {
          environment name: 'RUN_DOCKER_CONTAINER', value: 'true'
        }
        steps {
          script {
            def jbossContainerId = sh(returnStdout: true, script: 'docker container ls --filter name=jboss --quiet')
            if (jbossContainerId != '') {
              sh "docker container stop jboss"
              sh "docker container rm jboss"
            } else {
              jbossContainerId = sh(returnStdout: true, script: 'docker container ls -a --filter name=jboss --quiet')
              if (jbossContainerId != '') {
                sh "docker container rm jboss"
              }
            }
            sh "docker container run -d -p 8082:8080 -p 9990:9990 --name jboss ${DOCKER_REGISTRY_USER}/${ARTIFACT_ID}:${ARTIFACT_VERSION}"
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
