#!groovy

def dockerContainerRunMaven(mvnArgs){
  configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
    sh 'docker container run -t --rm --volume ${MAVEN_SETTINGS}:/tmp/settings.xml:ro --volume /maven/.m2:/root/.m2 --volume $PWD:/workspace --workdir /workspace maven:3.5.4-jdk-8-alpine mvn --batch-mode --settings /tmp/settings.xml ${mvnArgs}'
  }
}

pipeline {

  agent any

  environment {
    RUN_UNIT_TESTS = true
    ARTIFACT_ID = readMavenPom().getArtifactId()
    ARTIFACT_VERSION = readMavenPom().getVersion()
  }

  stages {

    stage 'Checkout Source' {
      steps {
        // https://jenkins.io/doc/pipeline/steps/git/
        def scmVars = checkout scm
        echo '==================================='
        echo 'Author : ${scmVars.GIT_AUTHOR_NAME}'
        echo 'Email : ${scmVars.GIT_AUTHOR_EMAIL}'
        echo 'Branch : ${scmVars.GIT_BRANCH}'
        echo 'Commit : ${scmVars.GIT_COMMIT}'
        echo '==================================='
      }
    }

    stage 'Build Package' {
      steps {
        if (RUN_UNIT_TESTS) {
          dockerContainerRunMaven 'clean package'
        } else {
          dockerContainerRunMaven 'clean package -DskipTests'
        }
      }
    }

    stage 'Deploy Artifact' {
      steps {
        dockerContainerRunMaven 'deploy -DskipTests'
      }
    }

    stage 'Deploy to environnement' {
      when {
        expression {
          echo "Should I deploy ?"
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
      always {
        echo "TODO send notification ${currentBuild.absoluteUrl} ${currentBuild.currentResult}"
      }
    }

  }
}
