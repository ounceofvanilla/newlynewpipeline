FROM debian@sha256:6e333bfc4b73914b5bfa9c83eb026ef6cea410ff3ba19d739fd9c598fb3a34b8
USER root
ADD image /
ARG user=jenkins
ARG group=jenkins
ARG uid=1000
ARG gid=1000
ARG JENKINS_AGENT_HOME=/home/${user}
ENV JENKINS_AGENT_HOME ${JENKINS_AGENT_HOME}
COPY image/qemu-arm-static /usr/bin/qemu-arm-static
RUN apt-get update \
    &&  apt-get install -y \
    build-essential=12.6 \
    cmake=3.13.4-1 \
    cppcheck=1.86-* \
    curl=7.64.* \
    ca-certificates=20190110* \
    fontconfig=2.13.* \
    git=1:2.20.* \
    gcc=4:8.3.0-1 \
    g++=4:8.3.0-1 \
    googletest=1.8.* \
    lcov=1.13-* \
    locales=2.28-* \
    make=4.2.1-1.2 \
    maven=3.6.0-1 \
    openjdk-8-jdk=8u252* \
    openssh-server=1:7.9* \
    python3=3.7.3-1 \
    python3-setuptools=40.8.0-1 \
    python3-pip=18.1-5 \
    python3-lxml=4.3.2-1 \
    python3-wheel=0.32.3-2 \
    python3-dev=3.7.3-1 \
    pylint=1.9.* \
    software-properties-common=0.96.* \
    sudo=1.8.* \
    unzip=6.0-* \
    -U pip \
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
    && curl -k https://repo.maven.apache.org/maven2/com/github/spotbugs/spotbugs/3.1.12/spotbugs-3.1.12.zip --output /opt/spotbugs.zip \
    && unzip /opt/spotbugs.zip -d /opt \
    && rm /opt/spotbugs.zip \
    && mv /opt/spotbugs-3.1.12 /opt/spotbugs \
    && ln -s /opt/spotbugs/bin/spotbugs /usr/bin/spotbugs \
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
    && ln -s /usr/src/googletest/googletest/include/gtest /usr/include/gtest \
    && cd /usr/src/googletest \
    && cmake CMakeLists.txt \
    && make \
    && mv googlemock/libg*.a /usr/lib \
    && mv googlemock/gtest/libg*.a /usr/lib \
    && python --version \
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
