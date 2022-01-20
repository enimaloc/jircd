node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
      withDockerContainer('gradle:jdk17') {
        withSonarQubeEnv() {
            sh "./gradlew sonarqube"
        }
      }
  }
}