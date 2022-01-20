pipeline {
  agent {
    docker {
      image 'gradle:jdk17'
      label 'gradle'
      reuseNode true
    }
  }
  stages {
    stage('SCM') {
      steps {
        checkout scm
      }
    }
    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv() {
          sh "./gradlew sonarqube"
        }
      }
    }
  }
}