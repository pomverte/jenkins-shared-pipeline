def call(Closure body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  echo "Pipeline configuration :\n${config}\n"

  // TODO define this map outside
  def projects = [
      'tailon-docker': 'http://gitea:3000/hvle/tailon-docker.git',
      'boot-external-config': 'http://gitea:3000/hvle/boot-external-config.git'
  ]
  def projectsKeySet = projects.keySet().join('\\n')

  pipeline {

    agent any

    parameters {
      choice(
          name: 'PROJECT',
          //choices: "${projectsKeySet}",
          choices: "tailon-docker\nboot-external-config",
          description: 'Project'
      )
    }

    options {
      buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
      stage('Checkout SCM') {
        steps {
          git url: projects.get(params.PROJECT)
          sh 'git config user.name jenkins'
          sh 'git config user.email jenkins@jenkins.ci'
        }
      }
      stage('jGitflow') {
        agent {
          docker {
            image 'maven:3.5.4-jdk-8-alpine'
            args '--net gitea_jenkins_default -v $HOME/.m2:/root/.m2'
            reuseNode true
          }
        }
        steps {
          runJGitflowGoal "${config.jgitflowGoal}"
        }
      }
    }

    post {
      always {
        notifySlack "${currentBuild.currentResult}", "#graylog-notifications"
        deleteDir()
      }
    }

  }
}
