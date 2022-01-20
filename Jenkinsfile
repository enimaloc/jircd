node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    tools {
       jdk "jdk-17"
    }
    withSonarQubeEnv() {
      sh "./gradlew sonarqube"
    }
  }
}