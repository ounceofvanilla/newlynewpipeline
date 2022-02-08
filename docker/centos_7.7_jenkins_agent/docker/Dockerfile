FROM centos:7.7.1908 as intermediate
USER root
RUN yum clean all \
    && yum install -y \
    curl-7.29.* \
    && mkdir /opt/coverity \
    && curl -k --output cov-analysis-linux64-2021.06.sh https://lnsvr0499.gcsd.harris.com:8443/downloads/cov-analysis-linux64-2021.06.sh \
    && curl -k --output /opt/coverity/analysis-license.dat https://lnsvr0499.gcsd.harris.com:8443/downloads/analysis-license.dat \
    && chmod +x cov-analysis-linux64-2021.06.sh \
    && ./cov-analysis-linux64-2021.06.sh -q --installation.dir=/opt/coverity --license.region=0 --license.agreement=agree --license.type.choice=0 --license.cov.path=/opt/coverity/analysis-license.dat

FROM centos:7.7.1908
COPY --from=intermediate /opt/coverity /opt/coverity
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'
USER root
ADD image /
ARG user=jenkins
ARG group=jenkins
ARG uid=1000
ARG gid=1000
ARG JENKINS_AGENT_HOME=/home/${user}
ARG USER_HOME_DIR=/home/${user}
ENV JENKINS_AGENT_HOME ${JENKINS_AGENT_HOME}
RUN yum clean all \
    && yum install -y \
    epel-release-7-11 \
    && yum install -y \
    curl-7.29.* \
    ca-certificates-* \
    cmake-2.8.12.2-2.el7 \
    cppcheck-2.6-* \
    crontabs-1.11-* \
    gcc-4.8.5-* \
    gcc-c++-4.8.5-* \
    git-1.8.* \
    gtest-1.6.* \
    gtest-devel-1.6.* \
    java-1.8.0-openjdk-1.8.* \
    lcov-1.13-* \
    make-1:3.82-24.el7.x86_64 \
    maven-3.0.5-17.el7 \
    openssh-server-7.4* \
    openssl-1.0* \
    qt5-qtbase-devel-5.9.7 \
    python3-3.6.8 \
    python3-setuptools-39.2.0-10.el7 \
    python3-pip-9.0.3-* \
    python3-lxml \
    python3-wheel-0.31.1-5.el7_7 \
    pylint-1.6.* \
    rpm-build-4.11.3 \
    redhat-rpm-config-9.1.0-88.el7.centos.noarch \
    sudo-1.8.* \
    unzip-6.0-* \
    which \
    zip-3.0-* \
    trivy \
    && pip3 install \
    -U pip \
    && pip3 install \
    -U pip \
    && pip3 install \
    -U pip \
    && pip3 install \
    'ansible==2.9.*' \
    'pytest==5.3.*' \
    'pytest-cov==2.8.*' \
    'coverage==4.5.*' \
    'unittest-xml-reporting==2.5.*' \
    'twine==3.1.*' \
    'bumpversion==0.5.*' \
    && curl https://repo.maven.apache.org/maven2/com/github/spotbugs/spotbugs/3.1.12/spotbugs-3.1.12.zip --output /opt/spotbugs.zip \
    && unzip /opt/spotbugs.zip -d /opt \
    && rm /opt/spotbugs.zip \
    && mv /opt/spotbugs-3.1.12 /opt/spotbugs \
    && ln -s /opt/spotbugs/bin/spotbugs /usr/bin/spotbugs \
    && curl https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.0.0.1744-linux.zip --output /opt/sonar-scanner.zip \
    && unzip /opt/sonar-scanner.zip -d /opt \
    && rm /opt/sonar-scanner.zip \
    && mv /opt/sonar-scanner-4.0.0.1744-linux /opt/sonar-scanner \
    && ln -s /opt/sonar-scanner/bin/sonar-scanner /usr/bin/sonar-scanner \
    && mkdir /opt/checkstyle \
    && mkdir /opt/checkstyle/docs \
    && curl -L https://github.com/checkstyle/checkstyle/releases/download/checkstyle-8.31/checkstyle-8.31-all.jar --output /opt/checkstyle/checkstyle-8.31-all.jar  \
    && mkdir /opt/zap \
    && ZAP_URL=$(curl -L -s https://github.com/zaproxy/zaproxy/releases/latest | grep "https.*tar.gz" | cut -d '"' -f 2,2) \
    && curl -L $ZAP_URL --output /opt/zap.tar.gz \
    && tar -xf /opt/zap.tar.gz -C /opt/zap --strip 1 \
    && rm /opt/zap.tar.gz \
    && ln -s /opt/zap/zap.sh /usr/bin/zap \
    && ln -s /opt/coverity/bin/cov-configure /usr/bin/cov-configure \
    && ln -s /opt/coverity/bin/cov-build /usr/bin/cov-build \
    && ln -s /opt/coverity/bin/cov-analyze /usr/bin/cov-analyze \
    && curl --location --url "https://services.gradle.org/distributions/gradle-7.2-bin.zip" --output /opt/gradle.zip \
    && unzip /opt/gradle.zip -d /opt \
    && rm /opt/gradle.zip \
    && mv /opt/gradle-7.2 /opt/gradle \
    && chmod +x /etc/profile.d/gradle.sh \
    && source /etc/profile.d/gradle.sh \
    && git --version \
    && ln -s /usr/bin/qmake-qt5 /usr/bin/qmake \
    && mvn -version \
    && make --version \
    && cmake --version \
    && qmake --version \
    && gcc --version \
    && g++ --version \
    && java -version \
    && cppcheck --version \
    && gcov --version \
    && lcov --version \
    && python --version \
    && sonar-scanner --version \
    && spotbugs -version \
    && java -jar /opt/checkstyle/checkstyle-8.31-all.jar -V \
    && zap -version \
    && trivy --version \
    && gradle --version \
    && pip3 list \
    && echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
    && groupadd -g ${gid} ${group} \
    && useradd -d "${JENKINS_AGENT_HOME}" -u "${uid}" -g "${gid}" -m -s /bin/bash "${user}" \
    && sed -i /etc/ssh/sshd_config \
        -e 's/#PermitRootLogin.*/PermitRootLogin no/' \
        -e 's/#RSAAuthentication.*/RSAAuthentication yes/'  \
        -e 's/#PasswordAuthentication.*/PasswordAuthentication no/' \
        -e 's/#SyslogFacility.*/SyslogFacility AUTH/' \
        -e 's/#LogLevel.*/LogLevel INFO/' \
    && ssh-keygen -A \
    && mkdir /var/run/sshd \
    && chmod 0755 /usr/local/bin/kube-startup \
    && chmod 0755 /usr/local/bin/update-certificates 

VOLUME "${JENKINS_AGENT_HOME}" "/tmp" "/run" "/var/run"
WORKDIR "${JENKINS_AGENT_HOME}"

EXPOSE 22

ENTRYPOINT ["docpf-startup"]