pipeline {
    agent { docker 'gradle:jdk17' }
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
