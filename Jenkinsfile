pipeline {
  agent any

  options {
    timestamps()
    timeout(time: 30, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20'))
    disableConcurrentBuilds()
  }

  environment {
    // Gradle 캐시를 Jenkins 홈 볼륨에 두면 컨테이너를 다시 만들어도 의존성을 다시 받지 않는다.
    GRADLE_USER_HOME = '/var/jenkins_home/.gradle'
    // CI에서는 데몬을 띄워 두지 않는다.
    GRADLE_OPTS = '-Dorg.gradle.daemon=false'
  }

  stages {
    stage('Info') {
      steps {
        sh 'git log -1 --oneline'
        sh 'java -version'
      }
    }

    stage('Verify') {
      steps {
        // --continue: 한 태스크가 실패해도 나머지를 계속 실행해서 모든 리포트를 만든다.
        // -PspotbugsReport=xml: Jenkins의 Warnings Next Generation 플러그인이 읽는 형식.
        sh './gradlew clean check --continue --console=plain -PspotbugsReport=xml'
      }
    }

    stage('Package') {
      steps {
        sh './gradlew bootJar --console=plain'
      }
    }
  }

  post {
    always {
      // 테스트 결과(JUnit XML) 집계
      junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true

      // 커버리지 추이와 소스 하이라이트 (Coverage 플러그인)
      recordCoverage(
        tools: [[parser: 'JACOCO', pattern: 'build/reports/jacoco/test/jacocoTestReport.xml']],
        sourceDirectories: [[path: 'src/main/java']],
        sourceCodeRetention: 'EVERY_BUILD'
      )

      // 정적 분석 경고 추이 (Warnings Next Generation 플러그인)
      recordIssues(
        enabledForFailure: true,
        tools: [
          checkStyle(pattern: 'build/reports/checkstyle/*.xml'),
          spotBugs(pattern: 'build/reports/spotbugs/*.xml', useRankAsPriority: true)
        ]
      )

      archiveArtifacts artifacts: 'build/reports/**', allowEmptyArchive: true
    }
    success {
      archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
    }
  }
}
