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
      checkout scm
    }
    stage('SonarQube Analysis') {
      withSonarQubeEnv() {
        sh "./gradlew sonarqube"
      }
    }
  }
}