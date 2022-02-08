FROM debian:10.1 as intermediate
USER root
RUN apt-get update \
    && apt-get install -y \
    curl=7.64.* \
    && mkdir /opt/coverity \
    && curl -k --output cov-analysis-linux64-2021.06.sh https://lnsvr0499.gcsd.harris.com:8443/downloads/cov-analysis-linux64-2021.06.sh \
    && curl -k --output /opt/coverity/analysis-license.dat https://lnsvr0499.gcsd.harris.com:8443/downloads/analysis-license.dat \
    && chmod +x cov-analysis-linux64-2021.06.sh \
    && ./cov-analysis-linux64-2021.06.sh -q --installation.dir=/opt/coverity --license.region=0 --license.agreement=agree --license.type.choice=0 --license.cov.path=/opt/coverity/analysis-license.dat

FROM debian:10.1
COPY --from=intermediate /opt/coverity /opt/coverity
USER root
ADD image /
ARG user=jenkins
ARG group=jenkins
ARG uid=1000
ARG gid=1000
ARG JENKINS_AGENT_HOME=/home/${user}
ARG USER_HOME_DIR=/home/${user}
ENV JENKINS_AGENT_HOME ${JENKINS_AGENT_HOME}
RUN apt-get update \
    && apt-get install -y \
    build-essential=12.6 \
    ca-certificates=* \
    cmake=3.13.4-1 \
    cppcheck=1.86-* \
    curl=7.64.* \
    fontconfig=2.13.* \
    gcc=4:8.3.0-1 \
    git=1:2.20.* \
    googletest=1.8.* \
    g++=4:8.3.0-1 \
    lcov=1.13-* \
    locales=2.28-* \
    make=4.2.1-1.2 \
    maven=3.6.0-1 \
    openjdk-8-jdk=8u252* \
    openssh-server=1:7.9*\
    python3=3.7.3-1 \
    python3-setuptools=40.8.* \
    python3-pip=18.* \
    python3-lxml=4.3.* \
    python3-wheel=0.32.* \
    python3-dev=3.7.3-* \
    pylint=1.9.* \
    qtbase5-dev-tools=5.7.1+dfsg-3+deb9u2 \
    qtchooser=63-g13a3d08-1 \
    libqt5core5a=5.7.1+dfsg-3+deb9u2 \
    libqt5dbus5=5.7.1+dfsg-3+deb9u2 \
    qtbase5-dev=5.7.1+dfsg-3+deb9u2 \
    libqt5concurrent5=5.7.1+dfsg-3+deb9u2 \
    libqt5gui5=5.7.1+dfsg-3+deb9u2 \
    libqt5network5=5.7.1+dfsg-3+deb9u2 \
    libqt5printsupport5=5.7.1+dfsg-3+deb9u2 \
    libqt5sql5=5.7.1+dfsg-3+deb9u2 \
    libqt5test5=5.7.1+dfsg-3+deb9u2 \
    libqt5widgets5=5.7.1+dfsg-3+deb9u2 \
    libqt5xml5=5.7.1+dfsg-3+deb9u2 \
    qt5-qmake=5.7.1+dfsg-3+deb9u2 \
    qt5-default=5.7.1+dfsg-3+deb9u2 \
    software-properties-common=0.96.* \
    sudo=1.8.* \
    unzip=6.0-* \
    zip=3.0-* \
    && curl https://aquasecurity.github.io/trivy-repo/deb/public.key | apt-key add \
    && echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee /etc/apt/sources.list.d/trivy.list \
    && apt-get update \
    && apt-get install -y trivy \
    && pip3 install \
    'ansible==2.9.*' \
    'pytest==5.3.*' \
    'pytest-cov==2.8.*' \
    'coverage==4.5.*' \
    'unittest-xml-reporting==2.5.*' \
    'twine==3.1.*' \
    'bumpversion==0.5.*' \
    && apt-get remove openjdk-11-jre-headless -y \
    && apt -y autoremove \
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
    && . /etc/profile.d/gradle.sh \
    && echo 'export PATH=/opt/gradle/bin:${PATH}' >> ~/.bashrc \
    && qmake --version \
    && mvn -version \
    && make --version \
    && cmake --version \
    && gcc --version \
    && g++ --version \
    && git --version \
    && java -version \
    && cppcheck --version \
    && gcov --version \
    && lcov --version \
    && java -jar /opt/checkstyle/checkstyle-8.31-all.jar -V \
    && zap -version \
    && trivy --version \
    && gradle --version \
    && ln -s /usr/src/googletest/googletest/include/gtest /usr/include/gtest \
    && cd /usr/src/googletest \
    && cmake CMakeLists.txt \
    && make \
    && mv googlemock/libg*.a /usr/lib \
    && mv googlemock/gtest/libg*.a /usr/lib \
    && python --version \
    && sonar-scanner --version \
    && spotbugs -version \
    && pip3 list \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g ${gid} ${group} \
    && useradd -d "${JENKINS_AGENT_HOME}" -u "${uid}" -g "${gid}" -m -s /bin/bash "${user}" \
    && sed -i /etc/ssh/sshd_config \
        -e 's/#PermitRootLogin.*/PermitRootLogin no/' \
        -e 's/#RSAAuthentication.*/RSAAuthentication yes/'  \
        -e 's/#PasswordAuthentication.*/PasswordAuthentication no/' \
        -e 's/#SyslogFacility.*/SyslogFacility AUTH/' \
        -e 's/#LogLevel.*/LogLevel INFO/' \
    && mkdir /var/run/sshd \
    && chmod 0755 /usr/local/bin/kube-startup \
    && chmod 0755 /usr/local/bin/update-certificates

VOLUME "${JENKINS_AGENT_HOME}" "/tmp" "/run" "/var/run"
WORKDIR "${JENKINS_AGENT_HOME}"

EXPOSE 22

ENTRYPOINT ["docpf-startup"]