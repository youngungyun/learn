# Jenkins LTS + JDK 25. 이 이미지의 JDK가 곧 빌드에 쓰는 JDK이므로 Gradle 툴체인(25)과 맞는다.
FROM jenkins/jenkins:lts-jdk25

USER root
# 호스트의 프로젝트 디렉터리를 읽기 전용으로 마운트해서 clone 할 때 소유자가 달라도 거부하지 않게 한다.
RUN git config --system --add safe.directory '*'

USER jenkins
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# 설정 파일은 볼륨이 덮어쓰지 않는 경로에 둔다.
COPY --chown=jenkins:jenkins casc/jenkins.yaml /usr/share/jenkins/casc/jenkins.yaml

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/casc/jenkins.yaml
